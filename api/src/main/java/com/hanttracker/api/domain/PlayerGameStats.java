package com.hanttracker.api.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/**
 * One player's result in one game, read from the {@code player_game_stats}
 * view (V2 migration). Read-only: games and rounds are still written through
 * {@link Game}.
 */
@Entity
@Immutable
@Table(name = "player_game_stats")
@IdClass(PlayerGameStats.Key.class)
@Getter
@NoArgsConstructor
public class PlayerGameStats {

    @Id
    private Long gameId;

    @Id
    private Long playerId;

    private int seat;

    private LocalDate playedOn;

    private boolean inProgress;

    /** Sum of the player's round points; low score wins. */
    private int points;

    /** 1 for the winner (or leader, while in progress); ties go to the earlier seat. */
    private int place;

    private int roundsPlayed;

    private int roundsWon;

    private int roundsOpened;

    private int roundsNotOpened;

    private int roundsQuit;

    /** Denars won (+) or paid (-); null until the money is entered. */
    private Integer moneyDen;

    public boolean isFinished() {
        return !inProgress;
    }

    /** Only a finished game has a winner. */
    public boolean isWon() {
        return isFinished() && place == 1;
    }

    public int moneyOrZero() {
        return moneyDen == null ? 0 : moneyDen;
    }

    public record Key(Long gameId, Long playerId) implements Serializable {

        public Key() {
            this(null, null);
        }
    }
}
