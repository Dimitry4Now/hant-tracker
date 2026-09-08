package com.hanttracker.api.seed;

import com.hanttracker.api.domain.AccountRequest;
import com.hanttracker.api.domain.Game;
import com.hanttracker.api.domain.MoneyEntry;
import com.hanttracker.api.domain.Round;
import com.hanttracker.api.domain.RoundEntryValue;
import com.hanttracker.api.domain.RoundOutcome;
import com.hanttracker.api.domain.UserAccount;
import com.hanttracker.api.domain.UserRole;
import com.hanttracker.api.domain.UserStatus;
import com.hanttracker.api.repo.AccountRequestRepository;
import com.hanttracker.api.repo.GameRepository;
import com.hanttracker.api.repo.UserAccountRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fills an empty database with the demo table the frontend was built against:
 * four regulars, a run of weekly games, and a couple of pending sign-ups.
 * Dates are anchored to the current week so the dashboard and the games chart
 * have something to show.
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DevDataSeeder implements CommandLineRunner {

    /** A full year of Thursdays, so the public games chart has every month. */
    private static final int WEEKS = 52;

    private final UserAccountRepository users;
    private final AccountRequestRepository requests;
    private final GameRepository games;
    private final PasswordEncoder encoder;
    private final String password;

    public DevDataSeeder(
            UserAccountRepository users,
            AccountRequestRepository requests,
            GameRepository games,
            PasswordEncoder encoder,
            @Value("${app.seed.password:password}") String password) {
        this.users = users;
        this.requests = requests;
        this.games = games;
        this.encoder = encoder;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (users.count() > 0) {
            return;
        }

        List<UserAccount> players =
                List.of(
                        user("Player A", "Player A", "playera@example.com", UserRole.ADMIN, UserStatus.ACTIVE),
                        user("Player B", "Player B", "playerb@example.com", UserRole.USER, UserStatus.ACTIVE),
                        user("Player C", "Player C", "playerc@example.com", UserRole.USER, UserStatus.ACTIVE),
                        user("Player D", "Player D", "playerd@example.com", UserRole.USER, UserStatus.LOCKED));
        users.saveAll(players);

        requests.saveAll(
                List.of(
                        request("Marko Petrovski", "Marko P.", "marko@example.com", 2),
                        request("Ivana Kostova", "Ivana K.", "ivana@example.com", 0)));

        List<Long> seats = players.stream().map(UserAccount::getId).toList();
        seedGames(seats);
    }

    private void seedGames(List<Long> seats) {
        LocalDate lastThursday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.THURSDAY));
        List<Game> seeded = new ArrayList<>();

        for (int week = 0; week < WEEKS; week++) {
            LocalDate playedOn = lastThursday.minusWeeks(week);

            // The most recent night is still going; every older one is finished.
            boolean inProgress = week == 0;
            Game game = game(playedOn, 1, seats, inProgress, week == 1 ? TRANSCRIPT_NOTE : null);
            if (week == 1) {
                transcriptRounds(game, seats);
                game.setMoney(
                        new ArrayList<>(
                                List.of(
                                        new MoneyEntry(seats.get(0), -50),
                                        new MoneyEntry(seats.get(1), -50),
                                        new MoneyEntry(seats.get(2), 150),
                                        new MoneyEntry(seats.get(3), -50))));
            } else {
                fillerRounds(game, seats, inProgress ? 12 : 9 + (week % 8), !inProgress);
            }
            seeded.add(game);

            // Some Thursdays run to a second game.
            if (week % 4 == 1) {
                Game second = game(playedOn, 2, seats, false, null);
                fillerRounds(second, seats, 11, true);
                seeded.add(second);
            }
        }

        games.saveAll(seeded);
    }

    private static final String TRANSCRIPT_NOTE =
            "Great Majstorska finish — Player C closed it out with a clean Hant.";

    /** The 24 Aug game from the GameDetail artboard, points as recorded. */
    private static final int[][] TRANSCRIPT = {
        {-50, 100, 100, 100},
        {100, -50, 70, 100},
        {100, 100, -50, 50},
        {-50, 80, 100, 100},
        {100, -50, 100, 100},
        {50, 100, -50, 100},
        {-50, 100, 60, 100},
        {100, 100, 100, -50},
        {-150, 140, 100, 100},
        {100, -50, 100, 50},
        {90, 100, -50, 100},
        {-50, 100, 100, 100},
        {100, 50, 100, -50},
        {100, 100, -50, 70},
        {-50, 100, 100, 100},
        {100, 100, -50, 100},
        {200, 200, -150, 200}
    };

    private void transcriptRounds(Game game, List<Long> seats) {
        for (int i = 0; i < TRANSCRIPT.length; i++) {
            boolean last = i == TRANSCRIPT.length - 1;
            boolean hant = i == 8 || last;
            Round round = round(game, i + 1, seats, TRANSCRIPT[i], hant, last);
            if (i == 8) {
                round.setComment("First Hant of the day!");
            } else if (last) {
                round.setComment("Majstorska — C closes it out with a Hant!");
            }
            game.addRound(round);
        }
        game.setMajstorska(true);
    }

    /** Deterministic rounds so list screens have plausible counts. */
    private void fillerRounds(Game game, List<Long> seats, int count, boolean withMajstorska) {
        for (int i = 0; i < count; i++) {
            boolean last = withMajstorska && i == count - 1;
            int[] points = last ? new int[] {200, 200, 200, 200} : new int[] {100, 100, 100, 100};
            points[i % seats.size()] = last ? -150 : -50;
            game.addRound(round(game, i + 1, seats, points, last, last));
        }
        if (withMajstorska) {
            game.setMajstorska(true);
        }
    }

    private Round round(Game game, int number, List<Long> seats, int[] points, boolean hant, boolean majstorska) {
        Round round = new Round();
        round.setGame(game);
        round.setNumber(number);
        round.setDealerId(seats.get((number - 1) % seats.size()));
        round.setHant(hant);
        round.setMajstorska(majstorska);

        List<RoundEntryValue> entries = new ArrayList<>();
        for (int seat = 0; seat < seats.size(); seat++) {
            int value = points[seat];
            RoundOutcome outcome = outcomeFor(value);
            entries.add(
                    new RoundEntryValue(
                            seats.get(seat), outcome, outcome == RoundOutcome.OPENED ? value : null, value));
        }
        round.setEntries(entries);
        return round;
    }

    private static RoundOutcome outcomeFor(int points) {
        if (points < 0) {
            return RoundOutcome.WINNER;
        }
        return points >= 100 ? RoundOutcome.NOT_OPENED : RoundOutcome.OPENED;
    }

    private Game game(
            LocalDate playedOn, int gameOfDay, List<Long> seats, boolean inProgress, String note) {
        Game game = new Game();
        game.setPlayedOn(playedOn);
        game.setGameOfDay(gameOfDay);
        game.setInProgress(inProgress);
        game.setNote(note);
        game.setPlayerIds(new ArrayList<>(seats));
        return game;
    }

    private UserAccount user(
            String fullName, String displayName, String email, UserRole role, UserStatus status) {
        UserAccount user = new UserAccount();
        user.setFullName(fullName);
        user.setDisplayName(displayName);
        user.setEmail(email);
        user.setPasswordHash(encoder.encode(password));
        user.setRole(role);
        user.setStatus(status);
        user.setJoinedOn(LocalDate.now().minusYears(2));
        return user;
    }

    private AccountRequest request(String fullName, String displayName, String email, int daysAgo) {
        AccountRequest request = new AccountRequest();
        request.setFullName(fullName);
        request.setDisplayName(displayName);
        request.setEmail(email);
        request.setPasswordHash(encoder.encode(password));
        request.setRequestedOn(LocalDate.now().minusDays(daysAgo));
        return request;
    }
}
