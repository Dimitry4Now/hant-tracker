package com.hanttracker.api.dto;

import com.hanttracker.api.domain.Game;
import com.hanttracker.api.domain.Round;
import java.time.LocalDate;
import java.util.Comparator;
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
                // Sorted here too: an edit can renumber rounds after they were loaded.
                game.getRounds().stream()
                        .sorted(Comparator.comparingInt(Round::getNumber))
                        .map(RoundDto::of)
                        .toList(),
                game.getMoney().stream().map(GameMoneyDto::of).toList());
    }
}
