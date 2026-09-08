package com.hanttracker.api.dto;

import com.hanttracker.api.domain.RoundEntryValue;
import com.hanttracker.api.domain.RoundOutcome;
import jakarta.validation.constraints.NotNull;

public record RoundEntryDto(
        @NotNull Long playerId, @NotNull RoundOutcome outcome, Integer cardValue, int points) {

    public static RoundEntryDto of(RoundEntryValue entry) {
        return new RoundEntryDto(
                entry.getPlayerId(), entry.getOutcome(), entry.getCardValue(), entry.getPoints());
    }

    public RoundEntryValue toEntity() {
        return new RoundEntryValue(playerId, outcome, cardValue, points);
    }
}
