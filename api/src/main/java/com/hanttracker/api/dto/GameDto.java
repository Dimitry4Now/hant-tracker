package com.hanttracker.api.dto;

import com.hanttracker.api.domain.Game;
import java.time.LocalDate;
import java.util.List;

public record GameDto(
        Long id,
        LocalDate playedOn,
        int gameOfDay,
        /** How many games share this date — the frontend labels "Game 2 of 2" with it. */
        int gamesThatDay,
        boolean inProgress,
        boolean majstorska,
        String note,
        List<Long> playerIds,
        List<RoundDto> rounds,
        List<GameMoneyDto> money) {

    public static GameDto of(Game game, int gamesThatDay) {
        return new GameDto(
                game.getId(),
                game.getPlayedOn(),
                game.getGameOfDay(),
                gamesThatDay,
                game.isInProgress(),
                game.isMajstorska(),
                game.getNote(),
                List.copyOf(game.getPlayerIds()),
                game.getRounds().stream().map(RoundDto::of).toList(),
                game.getMoney().stream().map(GameMoneyDto::of).toList());
    }
}
