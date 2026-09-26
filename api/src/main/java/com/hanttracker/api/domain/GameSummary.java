package com.hanttracker.api.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/** A game's round counts, read from the {@code game_summary} view (V2 migration). */
@Entity
@Immutable
@Table(name = "game_summary")
@Getter
@NoArgsConstructor
public class GameSummary {

    @Id
    private Long gameId;

    private LocalDate playedOn;

    private boolean inProgress;

    private int rounds;

    private int hantRounds;

    /** Not a Hant, and someone won it outright. */
    private int regularRounds;
}
