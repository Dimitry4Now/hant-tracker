package com.hanttracker.api.dto;

import com.hanttracker.api.domain.MoneyEntry;
import jakarta.validation.constraints.NotNull;

public record GameMoneyDto(@NotNull Long playerId, int amountDen) {

    public static GameMoneyDto of(MoneyEntry entry) {
        return new GameMoneyDto(entry.getPlayerId(), entry.getAmountDen());
    }

    public MoneyEntry toEntity() {
        return new MoneyEntry(playerId, amountDen);
    }
}
