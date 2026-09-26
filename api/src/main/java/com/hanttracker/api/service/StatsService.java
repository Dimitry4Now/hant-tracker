package com.hanttracker.api.service;

import static com.hanttracker.api.repo.spec.PlayedOnSpecs.playedIn;
import static com.hanttracker.api.repo.spec.PlayerGameStatsSpecs.forPlayer;
import static com.hanttracker.api.service.PlayerTotals.percent;

import com.hanttracker.api.domain.GameSummary;
import com.hanttracker.api.domain.PlayerGameStats;
import com.hanttracker.api.domain.UserAccount;
import com.hanttracker.api.dto.DashboardStatsDto;
import com.hanttracker.api.dto.LeaderboardRowDto;
import com.hanttracker.api.dto.MonthGamesDto;
import com.hanttracker.api.dto.PublicStatsDto;
import com.hanttracker.api.dto.PublicStatsDto.PlaystyleDto;
import com.hanttracker.api.dto.PublicStatsDto.RankingDto;
import com.hanttracker.api.repo.GameSummaryRepository;
import com.hanttracker.api.repo.PlayerGameStatsRepository;
import com.hanttracker.api.repo.UserAccountRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Derives every number the dashboard and the public landing page show. The
 * frontend renders these DTOs as they are, so the shapes match its models.
 *
 * <p>The per-game sums come from the {@code player_game_stats} and
 * {@code game_summary} views; this class only adds rows up. Game counts,
 * rounds and money include the game in progress; wins, losses and win rates
 * count finished games only.
 */
@Service
@Transactional(readOnly = true)
public class StatsService {

    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH);
    private static final DateTimeFormatter MONTH_YEAR =
            DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);
    private static final int MONTHS_SHOWN = 12;
    private static final int WEEK_BUCKETS = 5;

    private final PlayerGameStatsRepository stats;
    private final GameSummaryRepository summaries;
    private final UserAccountRepository users;

    public StatsService(
            PlayerGameStatsRepository stats, GameSummaryRepository summaries, UserAccountRepository users) {
        this.stats = stats;
        this.summaries = summaries;
        this.users = users;
    }

    // --- dashboard -------------------------------------------------------

    public DashboardStatsDto dashboardFor(UserAccount user) {
        YearMonth thisMonth = YearMonth.now();
        Specification<PlayerGameStats> mine = forPlayer(user.getId());

        PlayerTotals allTime = PlayerTotals.of(stats.findAll(mine));
        PlayerTotals current = PlayerTotals.of(stats.findAll(mine.and(playedIn(thisMonth))));
        PlayerTotals previous = PlayerTotals.of(stats.findAll(mine.and(playedIn(thisMonth.minusMonths(1)))));

        return new DashboardStatsDto(
                current.games(),
                previous.games(),
                current.netDen(),
                previous.netDen(),
                current.winRate(),
                previous.winRate(),
                allTime.winRate(),
                allTime.games(),
                allTime.netDen(),
                leaderboard());
    }

    private List<LeaderboardRowDto> leaderboard() {
        Map<Long, PlayerTotals> totals = totalsByPlayer();
        List<LeaderboardRowDto> rows = new ArrayList<>();
        for (UserAccount player : users.findAllByOrderByIdAsc()) {
            PlayerTotals t = totals.getOrDefault(player.getId(), PlayerTotals.NONE);
            rows.add(
                    new LeaderboardRowDto(
                            player.getId(),
                            player.getDisplayName(),
                            t.wins(),
                            t.losses(),
                            t.rounds(),
                            t.roundWinRate(),
                            t.netDen()));
        }
        rows.sort(
                Comparator.<LeaderboardRowDto>comparingInt(LeaderboardRowDto::wins)
                        .thenComparingInt(LeaderboardRowDto::netDen)
                        .reversed());
        return rows;
    }

    // --- public landing --------------------------------------------------

    public PublicStatsDto publicStats() {
        List<GameSummary> games = summaries.findAll(Specification.unrestricted());
        List<UserAccount> players = users.findAllByOrderByIdAsc();
        Map<Long, PlayerTotals> totals = totalsByPlayer();

        int rounds = games.stream().mapToInt(GameSummary::getRounds).sum();
        int hantRounds = games.stream().mapToInt(GameSummary::getHantRounds).sum();
        int regularRounds = games.stream().mapToInt(GameSummary::getRegularRounds).sum();
        YearMonth end = YearMonth.now();
        YearMonth start = end.minusMonths(MONTHS_SHOWN - 1L);

        return new PublicStatsDto(
                startYear(games),
                "%s – %s".formatted(start.format(MONTH_YEAR), end.format(MONTH_YEAR)),
                gamesByMonth(games, start),
                games.size(),
                rounds,
                hantRounds,
                players.size(),
                percent(hantRounds, rounds),
                percent(regularRounds, rounds),
                rankings(players, totals),
                playstyles(players, totals));
    }

    private int startYear(List<GameSummary> games) {
        return games.stream()
                .map(GameSummary::getPlayedOn)
                .min(LocalDate::compareTo)
                .map(LocalDate::getYear)
                .orElse(LocalDate.now().getYear());
    }

    /** One bar per month, each split into the week of the month it was played in. */
    private List<MonthGamesDto> gamesByMonth(List<GameSummary> games, YearMonth start) {
        List<MonthGamesDto> months = new ArrayList<>();
        for (int i = 0; i < MONTHS_SHOWN; i++) {
            YearMonth month = start.plusMonths(i);
            List<Integer> weeks = new ArrayList<>(List.of(0, 0, 0, 0, 0));
            for (GameSummary game : games) {
                if (YearMonth.from(game.getPlayedOn()).equals(month)) {
                    int bucket = Math.min((game.getPlayedOn().getDayOfMonth() - 1) / 7, WEEK_BUCKETS - 1);
                    weeks.set(bucket, weeks.get(bucket) + 1);
                }
            }
            months.add(new MonthGamesDto(month.atDay(1).format(MONTH), weeks));
        }
        return months;
    }

    private List<RankingDto> rankings(List<UserAccount> players, Map<Long, PlayerTotals> totals) {
        return players.stream()
                .map(
                        player -> {
                            PlayerTotals t = totals.getOrDefault(player.getId(), PlayerTotals.NONE);
                            return new RankingDto(
                                    player.getDisplayName(), t.games(), t.winRate(), t.rounds(), t.roundWinRate());
                        })
                .sorted(
                        Comparator.<RankingDto>comparingInt(RankingDto::games)
                                .thenComparingInt(RankingDto::winRate)
                                .reversed())
                .toList();
    }

    /** A one-word read on how someone plays, from the mix of their round outcomes. */
    private List<PlaystyleDto> playstyles(List<UserAccount> players, Map<Long, PlayerTotals> totals) {
        return players.stream()
                .map(
                        player ->
                                new PlaystyleDto(
                                        player.getDisplayName(),
                                        styleFor(totals.getOrDefault(player.getId(), PlayerTotals.NONE))))
                .toList();
    }

    private String styleFor(PlayerTotals t) {
        if (t.games() < 10) {
            return "Newcomer";
        }
        if (percent(t.roundsQuit(), t.rounds()) >= 10) {
            return "Early exit";
        }
        if (t.winRate() >= 30) {
            return "Closer";
        }
        if (percent(t.roundsOpened(), t.rounds()) >= 40) {
            return "Risk-taker";
        }
        if (percent(t.roundsNotOpened(), t.rounds()) >= 60) {
            return "Cautious";
        }
        return "Steady";
    }

    // --- shared helpers --------------------------------------------------

    /** Every player's all-time totals, from one read of the view. */
    private Map<Long, PlayerTotals> totalsByPlayer() {
        return stats.findAll(Specification.unrestricted()).stream()
                .collect(
                        Collectors.groupingBy(
                                PlayerGameStats::getPlayerId,
                                Collectors.collectingAndThen(Collectors.toList(), PlayerTotals::of)));
    }
}
