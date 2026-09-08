package com.hanttracker.api.service;

import com.hanttracker.api.domain.Game;
import com.hanttracker.api.domain.MoneyEntry;
import com.hanttracker.api.domain.Round;
import com.hanttracker.api.domain.RoundEntryValue;
import com.hanttracker.api.domain.RoundOutcome;
import com.hanttracker.api.domain.UserAccount;
import com.hanttracker.api.dto.DashboardStatsDto;
import com.hanttracker.api.dto.LeaderboardRowDto;
import com.hanttracker.api.dto.MonthGamesDto;
import com.hanttracker.api.dto.PublicStatsDto;
import com.hanttracker.api.dto.PublicStatsDto.PlaystyleDto;
import com.hanttracker.api.dto.PublicStatsDto.RankingDto;
import com.hanttracker.api.repo.GameRepository;
import com.hanttracker.api.repo.UserAccountRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Derives every number the dashboard and the public landing page show. The
 * frontend renders these DTOs as they are, so the shapes match its models.
 */
@Service
@Transactional(readOnly = true)
public class StatsService {

    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH);
    private static final DateTimeFormatter MONTH_YEAR =
            DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);
    private static final int MONTHS_SHOWN = 12;
    private static final int WEEK_BUCKETS = 5;

    private final GameRepository games;
    private final UserAccountRepository users;

    public StatsService(GameRepository games, UserAccountRepository users) {
        this.games = games;
        this.users = users;
    }

    // --- dashboard -------------------------------------------------------

    public DashboardStatsDto dashboardFor(UserAccount user) {
        List<Game> all = games.findAll();
        YearMonth thisMonth = YearMonth.now();
        YearMonth lastMonth = thisMonth.minusMonths(1);

        List<Game> mine = all.stream().filter(g -> g.getPlayerIds().contains(user.getId())).toList();
        List<Game> minesThisMonth = inMonth(mine, thisMonth);
        List<Game> minesLastMonth = inMonth(mine, lastMonth);

        return new DashboardStatsDto(
                minesThisMonth.size(),
                minesLastMonth.size(),
                netDen(minesThisMonth, user.getId()),
                netDen(minesLastMonth, user.getId()),
                winRate(minesThisMonth, user.getId()),
                winRate(minesLastMonth, user.getId()),
                winRate(mine, user.getId()),
                mine.size(),
                netDen(mine, user.getId()),
                leaderboard(all));
    }

    private List<LeaderboardRowDto> leaderboard(List<Game> all) {
        List<LeaderboardRowDto> rows = new ArrayList<>();
        for (UserAccount player : users.findAllByOrderByIdAsc()) {
            List<Game> played =
                    all.stream().filter(g -> g.getPlayerIds().contains(player.getId())).toList();
            List<Game> finished = played.stream().filter(g -> !g.isInProgress()).toList();
            int wins = (int) finished.stream().filter(g -> won(g, player.getId())).count();
            rows.add(
                    new LeaderboardRowDto(
                            player.getId(),
                            player.getDisplayName(),
                            played.stream().mapToInt(g -> totalPoints(g, player.getId())).sum(),
                            wins,
                            finished.size() - wins,
                            netDen(played, player.getId())));
        }
        rows.sort(
                Comparator.<LeaderboardRowDto>comparingInt(LeaderboardRowDto::wins)
                        .thenComparingInt(LeaderboardRowDto::netDen)
                        .reversed());
        return rows;
    }

    // --- public landing --------------------------------------------------

    public PublicStatsDto publicStats() {
        List<Game> all = games.findAll();
        List<Round> rounds = all.stream().flatMap(g -> g.getRounds().stream()).toList();
        List<UserAccount> players = users.findAllByOrderByIdAsc();

        int hantRounds = (int) rounds.stream().filter(Round::isHant).count();
        int regularRounds =
                (int)
                        rounds.stream()
                                .filter(r -> !r.isHant() && hasOutcome(r, RoundOutcome.WINNER))
                                .count();
        int quitterGames =
                (int)
                        all.stream()
                                .filter(g -> g.getRounds().stream().anyMatch(r -> hasOutcome(r, RoundOutcome.QUIT)))
                                .count();

        YearMonth end = YearMonth.now();
        YearMonth start = end.minusMonths(MONTHS_SHOWN - 1L);

        return new PublicStatsDto(
                startYear(all),
                "%s – %s".formatted(start.format(MONTH_YEAR), end.format(MONTH_YEAR)),
                gamesByMonth(all, start),
                all.size(),
                rounds.size(),
                hantRounds,
                players.size(),
                percent(hantRounds, rounds.size()),
                percent(regularRounds, rounds.size()),
                percent(quitterGames, all.size()),
                rankings(all, players),
                playstyles(all, players));
    }

    private int startYear(List<Game> all) {
        return all.stream()
                .map(Game::getPlayedOn)
                .min(LocalDate::compareTo)
                .map(LocalDate::getYear)
                .orElse(LocalDate.now().getYear());
    }

    /** One bar per month, each split into the week of the month it was played in. */
    private List<MonthGamesDto> gamesByMonth(List<Game> all, YearMonth start) {
        List<MonthGamesDto> months = new ArrayList<>();
        for (int i = 0; i < MONTHS_SHOWN; i++) {
            YearMonth month = start.plusMonths(i);
            List<Integer> weeks = new ArrayList<>(List.of(0, 0, 0, 0, 0));
            for (Game game : inMonth(all, month)) {
                int bucket = Math.min((game.getPlayedOn().getDayOfMonth() - 1) / 7, WEEK_BUCKETS - 1);
                weeks.set(bucket, weeks.get(bucket) + 1);
            }
            months.add(new MonthGamesDto(month.atDay(1).format(MONTH), weeks));
        }
        return months;
    }

    private List<RankingDto> rankings(List<Game> all, List<UserAccount> players) {
        return players.stream()
                .map(
                        player -> {
                            List<Game> played =
                                    all.stream().filter(g -> g.getPlayerIds().contains(player.getId())).toList();
                            return new RankingDto(
                                    player.getDisplayName(), played.size(), winRate(played, player.getId()));
                        })
                .sorted(
                        Comparator.<RankingDto>comparingInt(RankingDto::games)
                                .thenComparingInt(RankingDto::winRate)
                                .reversed())
                .toList();
    }

    /** A one-word read on how someone plays, from the mix of their round outcomes. */
    private List<PlaystyleDto> playstyles(List<Game> all, List<UserAccount> players) {
        List<PlaystyleDto> styles = new ArrayList<>();
        for (UserAccount player : players) {
            List<Game> played =
                    all.stream().filter(g -> g.getPlayerIds().contains(player.getId())).toList();
            List<RoundEntryValue> entries =
                    played.stream()
                            .flatMap(g -> g.getRounds().stream())
                            .flatMap(r -> r.getEntries().stream())
                            .filter(e -> e.getPlayerId().equals(player.getId()))
                            .toList();

            styles.add(new PlaystyleDto(player.getDisplayName(), styleFor(played, entries, player.getId())));
        }
        return styles;
    }

    private String styleFor(List<Game> played, List<RoundEntryValue> entries, Long playerId) {
        if (played.size() < 10) {
            return "Newcomer";
        }
        int rounds = entries.size();
        int quits = countOutcome(entries, RoundOutcome.QUIT);
        int notOpened = countOutcome(entries, RoundOutcome.NOT_OPENED);
        int opened = countOutcome(entries, RoundOutcome.OPENED);

        if (percent(quits, rounds) >= 10) {
            return "Early exit";
        }
        if (winRate(played, playerId) >= 30) {
            return "Closer";
        }
        if (percent(opened, rounds) >= 40) {
            return "Risk-taker";
        }
        if (percent(notOpened, rounds) >= 60) {
            return "Cautious";
        }
        return "Steady";
    }

    // --- shared helpers --------------------------------------------------

    private static List<Game> inMonth(List<Game> games, YearMonth month) {
        return games.stream().filter(g -> YearMonth.from(g.getPlayedOn()).equals(month)).toList();
    }

    private static int netDen(List<Game> games, Long playerId) {
        return games.stream()
                .flatMap(g -> g.getMoney().stream())
                .filter(m -> m.getPlayerId().equals(playerId))
                .mapToInt(MoneyEntry::getAmountDen)
                .sum();
    }

    /** Share of that player's finished games they won, as a whole percent. */
    private static int winRate(List<Game> games, Long playerId) {
        List<Game> finished = games.stream().filter(g -> !g.isInProgress()).toList();
        long wins = finished.stream().filter(g -> won(g, playerId)).count();
        return percent((int) wins, finished.size());
    }

    /** Low score wins, so the winner is whoever finished with the fewest points. */
    private static boolean won(Game game, Long playerId) {
        Optional<Long> winner =
                game.getPlayerIds().stream()
                        .min(Comparator.comparingInt((Long id) -> totalPoints(game, id)));
        return winner.filter(playerId::equals).isPresent();
    }

    private static int totalPoints(Game game, Long playerId) {
        return game.getRounds().stream()
                .flatMap(r -> r.getEntries().stream())
                .filter(e -> e.getPlayerId().equals(playerId))
                .mapToInt(RoundEntryValue::getPoints)
                .sum();
    }

    private static boolean hasOutcome(Round round, RoundOutcome outcome) {
        return round.getEntries().stream().anyMatch(e -> e.getOutcome() == outcome);
    }

    private static int countOutcome(List<RoundEntryValue> entries, RoundOutcome outcome) {
        return (int) entries.stream().filter(e -> e.getOutcome() == outcome).count();
    }

    private static int percent(int part, int total) {
        return total == 0 ? 0 : Math.round(part * 100f / total);
    }
}
