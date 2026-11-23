package com.hanttracker.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.hanttracker.api.domain.RoundOutcome;
import com.hanttracker.api.domain.UserAccount;
import com.hanttracker.api.domain.UserRole;
import com.hanttracker.api.dto.DashboardStatsDto;
import com.hanttracker.api.dto.GameDto;
import com.hanttracker.api.dto.GameDtos.CreateGameRequest;
import com.hanttracker.api.dto.GameDtos.CreateRoundRequest;
import com.hanttracker.api.dto.GameMoneyDto;
import com.hanttracker.api.dto.LeaderboardRowDto;
import com.hanttracker.api.dto.PublicStatsDto;
import com.hanttracker.api.dto.PublicStatsDto.PlaystyleDto;
import com.hanttracker.api.dto.PublicStatsDto.RankingDto;
import com.hanttracker.api.dto.RoundEntryDto;
import com.hanttracker.api.repo.UserAccountRepository;
import com.hanttracker.api.support.EmptyDatabase;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Three players and three games with known results:
 *
 * <ul>
 *   <li>G1, this month, finished: Bob wins (0 points), Cid second (70), Ada
 *       last (150). Money: Bob +300, Ada −200, Cid −100. Round 2 is a Hant.
 *   <li>G2, last month, finished: Ada wins over Bob. Money: Ada +100, Bob −100.
 *   <li>G3, this month, in progress: Cid leads Ada; no money yet.
 * </ul>
 */
@EmptyDatabase
class StatsServiceTests {

    @Autowired
    private StatsService stats;

    @Autowired
    private GameService games;

    @Autowired
    private UserAccountRepository users;

    @Autowired
    private EntityManager entityManager;

    private UserAccount ada;
    private UserAccount bob;
    private UserAccount cid;

    @BeforeEach
    void playThreeGames() {
        ada = player("Ada");
        bob = player("Bob");
        cid = player("Cid");
        YearMonth thisMonth = YearMonth.now();

        GameDto g1 = games.create(new CreateGameRequest(thisMonth.atDay(1), ids(ada, bob, cid), null));
        games.addRound(
                g1.id(),
                round(
                        1,
                        false,
                        entry(ada, RoundOutcome.WINNER, -50),
                        entry(bob, RoundOutcome.NOT_OPENED, 100),
                        entry(cid, RoundOutcome.OPENED, 30)));
        games.addRound(
                g1.id(),
                round(
                        2,
                        true,
                        entry(bob, RoundOutcome.WINNER, -100),
                        entry(ada, RoundOutcome.NOT_OPENED, 200),
                        entry(cid, RoundOutcome.QUIT, 40)));
        games.saveMoney(g1.id(), List.of(money(bob, 300), money(ada, -200), money(cid, -100)));
        games.setInProgress(g1.id(), false);

        GameDto g2 = games.create(new CreateGameRequest(thisMonth.minusMonths(1).atDay(15), ids(ada, bob), null));
        games.addRound(
                g2.id(), round(1, false, entry(ada, RoundOutcome.WINNER, -50), entry(bob, RoundOutcome.OPENED, 20)));
        games.saveMoney(g2.id(), List.of(money(ada, 100), money(bob, -100)));
        games.setInProgress(g2.id(), false);

        GameDto g3 = games.create(new CreateGameRequest(thisMonth.atDay(2), ids(ada, cid), null));
        games.addRound(
                g3.id(), round(1, false, entry(cid, RoundOutcome.WINNER, -50), entry(ada, RoundOutcome.OPENED, 10)));

        // The stats read database views, which Hibernate doesn't know depend on
        // these tables, so it won't flush them first. In the app each request
        // commits on its own; here everything shares one transaction.
        entityManager.flush();
    }

    @Test
    void theDashboardCountsThisMonthLastMonthAndAllTime() {
        DashboardStatsDto dashboard = stats.dashboardFor(ada);

        assertThat(dashboard.gamesThisMonth()).isEqualTo(2);
        assertThat(dashboard.gamesLastMonth()).isEqualTo(1);
        // G3 has no money yet, so it counts as 0.
        assertThat(dashboard.netDenThisMonth()).isEqualTo(-200);
        assertThat(dashboard.netDenLastMonth()).isEqualTo(100);
        // Only finished games have a winner: this month that is G1, which Ada lost.
        assertThat(dashboard.winRateThisMonth()).isZero();
        assertThat(dashboard.winRateLastMonth()).isEqualTo(100);
        assertThat(dashboard.allTimeWinRate()).isEqualTo(50);
        assertThat(dashboard.allTimeGames()).isEqualTo(3);
        assertThat(dashboard.allTimeNetDen()).isEqualTo(-100);
    }

    @Test
    void theLeaderboardRanksByWinsThenMoney() {
        List<LeaderboardRowDto> board = stats.dashboardFor(ada).leaderboard();

        assertThat(board).extracting(LeaderboardRowDto::name).containsExactly("Bob", "Ada", "Cid");
        LeaderboardRowDto bobRow = board.getFirst();
        assertThat(bobRow.wins()).isEqualTo(1);
        assertThat(bobRow.losses()).isEqualTo(1);
        assertThat(bobRow.rounds()).isEqualTo(3);
        assertThat(bobRow.roundWinRate()).isEqualTo(33);
        assertThat(bobRow.netDen()).isEqualTo(200);
        // A game still in progress is neither a win nor a loss.
        LeaderboardRowDto cidRow = board.get(2);
        assertThat(cidRow.wins()).isZero();
        assertThat(cidRow.losses()).isEqualTo(1);
    }

    @Test
    void thePublicPageAddsUpGamesRoundsAndPlayers() {
        PublicStatsDto page = stats.publicStats();

        assertThat(page.gamesPlayed()).isEqualTo(3);
        assertThat(page.roundsRecorded()).isEqualTo(4);
        assertThat(page.hantsRecorded()).isEqualTo(1);
        assertThat(page.players()).isEqualTo(3);
        assertThat(page.roundsEndingInHant()).isEqualTo(25);
        assertThat(page.roundsClosedRegular()).isEqualTo(75);
        assertThat(page.startYear()).isEqualTo(YearMonth.now().minusMonths(1).getYear());

        // Twelve months, this one last; G1 and G3 fall in its first week, G2 in last month's third.
        assertThat(page.gamesByMonth()).hasSize(12);
        assertThat(page.gamesByMonth().getLast().weeks()).containsExactly(2, 0, 0, 0, 0);
        assertThat(page.gamesByMonth().get(10).weeks()).containsExactly(0, 0, 1, 0, 0);

        assertThat(page.rankings()).extracting(RankingDto::name).containsExactly("Ada", "Bob", "Cid");
        assertThat(page.rankings().getFirst().games()).isEqualTo(3);
        // Under ten games everyone is still a newcomer.
        assertThat(page.playstyles()).extracting(PlaystyleDto::style).containsOnly("Newcomer");
    }

    private UserAccount player(String name) {
        UserAccount user = new UserAccount();
        user.setFullName(name);
        user.setDisplayName(name);
        user.setEmail(name.toLowerCase() + "@example.com");
        user.setPasswordHash("x");
        user.setRole(UserRole.USER);
        user.setJoinedOn(LocalDate.now());
        return users.save(user);
    }

    private static List<Long> ids(UserAccount... players) {
        return java.util.Arrays.stream(players).map(UserAccount::getId).toList();
    }

    private static RoundEntryDto entry(UserAccount player, RoundOutcome outcome, int points) {
        return new RoundEntryDto(player.getId(), outcome, outcome == RoundOutcome.OPENED ? 10 : null, points);
    }

    private CreateRoundRequest round(int number, boolean hant, RoundEntryDto... entries) {
        return new CreateRoundRequest(number, entries[0].playerId(), false, hant, null, List.of(entries));
    }

    private static GameMoneyDto money(UserAccount player, int den) {
        return new GameMoneyDto(player.getId(), den);
    }
}
