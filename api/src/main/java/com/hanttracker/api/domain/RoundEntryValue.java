package com.hanttracker.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One player's result in one round. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoundEntryValue {

    @Column(nullable = false)
    private Long playerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoundOutcome outcome;

    /** Only meaningful for OPENED — the value of the cards left in hand. */
    private Integer cardValue;

    @Column(nullable = false)
    private int points;
}
