package com.hanttracker.api.service;

import com.hanttracker.api.domain.PlayerGameStats;
import java.util.Collection;

/**
 * A player's {@link PlayerGameStats} rows added up. Which games count is up to
 * the specification the rows were loaded with.
 *
 * @param games every game, in progress included
 * @param finishedGames games that have ended, the only ones with a winner
 */
record PlayerTotals(
        int games,
        int finishedGames,
        int wins,
        int rounds,
        int roundWins,
        int roundsOpened,
        int roundsNotOpened,
        int roundsQuit,
        int netDen) {

    /** Someone who has not played yet. */
    static final PlayerTotals NONE = new PlayerTotals(0, 0, 0, 0, 0, 0, 0, 0, 0);

    static PlayerTotals of(Collection<PlayerGameStats> rows) {
        int finished = 0;
        int wins = 0;
        int rounds = 0;
        int roundWins = 0;
        int opened = 0;
        int notOpened = 0;
        int quit = 0;
        int net = 0;
        for (PlayerGameStats row : rows) {
            finished += row.isFinished() ? 1 : 0;
            wins += row.isWon() ? 1 : 0;
            rounds += row.getRoundsPlayed();
            roundWins += row.getRoundsWon();
            opened += row.getRoundsOpened();
            notOpened += row.getRoundsNotOpened();
            quit += row.getRoundsQuit();
            net += row.moneyOrZero();
        }
        return new PlayerTotals(rows.size(), finished, wins, rounds, roundWins, opened, notOpened, quit, net);
    }

    int losses() {
        return finishedGames - wins;
    }

    /** Share of finished games won, as a whole percent. */
    int winRate() {
        return percent(wins, finishedGames);
    }

    /** Share of rounds won, as a whole percent. */
    int roundWinRate() {
        return percent(roundWins, rounds);
    }

    static int percent(int part, int total) {
        return total == 0 ? 0 : Math.round(part * 100f / total);
    }
}
