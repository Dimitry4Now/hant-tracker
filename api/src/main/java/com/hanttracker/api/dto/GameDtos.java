package com.hanttracker.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/** Request bodies for /api/games. */
public final class GameDtos {

    private GameDtos() {}

    public record CreateGameRequest(
            @NotNull LocalDate playedOn, @NotEmpty List<Long> playerIds, String note) {}

    public record CreateRoundRequest(
            int number,
            @NotNull Long dealerId,
            boolean majstorska,
            boolean hant,
            String comment,
            @NotEmpty @Valid List<RoundEntryDto> entries) {}

    public record SaveMoneyRequest(@NotNull @Valid List<GameMoneyDto> money) {}

    public record FinishGameRequest(boolean inProgress) {}
}
