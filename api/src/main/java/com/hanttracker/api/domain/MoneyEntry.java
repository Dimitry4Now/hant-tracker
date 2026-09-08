package com.hanttracker.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Whole denars settled by hand after a game. Euro is display-only. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MoneyEntry {

    @Column(nullable = false)
    private Long playerId;

    @Column(nullable = false)
    private int amountDen;
}
