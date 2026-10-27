package com.hanttracker.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hanttracker.api.domain.RoundOutcome;
import com.hanttracker.api.dto.GameDto;
import com.hanttracker.api.dto.GameDtos.CreateGameRequest;
import com.hanttracker.api.dto.GameDtos.CreateRoundRequest;
import com.hanttracker.api.dto.GameDtos.ReplaceRoundsRequest;
import com.hanttracker.api.dto.RoundEntryDto;
import com.hanttracker.api.web.ConflictException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class GameServiceReplaceRoundsTests {

    @Autowired
    private GameService games;

    @Test
    void storesEveryRoundAndClosesTheGame() {
        GameDto game = newGame();

        GameDto saved = games.replaceRounds(
                game.id(), new ReplaceRoundsRequest(List.of(round(1, false), round(2, false)), true));

        assertThat(saved.rounds()).extracting(r -> r.number()).containsExactly(1, 2);
        assertThat(saved.inProgress()).isFalse();
        assertThat(saved.majstorska()).isFalse();
    }

    @Test
    void aMajstorskaClosesTheGameEvenWhenNotMarkedFinished() {
        GameDto game = newGame();

        GameDto saved = games.replaceRounds(
                game.id(), new ReplaceRoundsRequest(List.of(round(1, false), round(2, true)), false));

        assertThat(saved.majstorska()).isTrue();
        assertThat(saved.inProgress()).isFalse();
    }

    @Test
    void refusesAGameThatAlreadyHasRounds() {
        GameDto game = newGame();
        games.addRound(game.id(), round(1, false));

        assertThatThrownBy(() -> games.replaceRounds(
                        game.id(), new ReplaceRoundsRequest(List.of(round(2, false)), true)))
                .isInstanceOf(ConflictException.class);
    }

    private GameDto newGame() {
        return games.create(new CreateGameRequest(LocalDate.of(2026, 8, 18), List.of(1L, 2L), null));
    }

    private static CreateRoundRequest round(int number, boolean majstorska) {
        return new CreateRoundRequest(
                number,
                1L,
                majstorska,
                majstorska,
                null,
                List.of(
                        new RoundEntryDto(1L, RoundOutcome.WINNER, null, majstorska ? -150 : -50),
                        new RoundEntryDto(2L, RoundOutcome.NOT_OPENED, null, majstorska ? 200 : 100)));
    }
}
