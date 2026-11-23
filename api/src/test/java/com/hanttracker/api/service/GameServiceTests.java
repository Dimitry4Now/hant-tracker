package com.hanttracker.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hanttracker.api.domain.RoundOutcome;
import com.hanttracker.api.dto.GameDto;
import com.hanttracker.api.dto.GameDtos.CreateGameRequest;
import com.hanttracker.api.dto.GameDtos.CreateRoundRequest;
import com.hanttracker.api.dto.GameDtos.UpdateGameRequest;
import com.hanttracker.api.dto.GameMoneyDto;
import com.hanttracker.api.dto.RoundDto;
import com.hanttracker.api.dto.RoundEntryDto;
import com.hanttracker.api.support.EmptyDatabase;
import com.hanttracker.api.web.ConflictException;
import com.hanttracker.api.web.NotFoundException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@EmptyDatabase
class GameServiceTests {

    private static final LocalDate TUESDAY = LocalDate.of(2026, 9, 1);
    private static final LocalDate WEDNESDAY = LocalDate.of(2026, 9, 2);

    @Autowired
    private GameService games;

    private GameDto game(LocalDate day, Long... players) {
        return games.create(new CreateGameRequest(day, List.of(players), null));
    }

    private static CreateRoundRequest round(int number, boolean majstorska) {
        return new CreateRoundRequest(
                number,
                1L,
                majstorska,
                false,
                null,
                List.of(
                        new RoundEntryDto(1L, RoundOutcome.WINNER, null, -50),
                        new RoundEntryDto(2L, RoundOutcome.NOT_OPENED, null, 100)));
    }

    @Test
    void gamesOnTheSameDayAreNumbered() {
        GameDto first = game(TUESDAY, 1L, 2L);
        GameDto second = game(TUESDAY, 1L, 2L);
        GameDto other = game(WEDNESDAY, 1L, 2L);

        assertThat(first.gameOfDay()).isEqualTo(1);
        assertThat(second.gameOfDay()).isEqualTo(2);
        assertThat(games.get(first.id()).gamesThatDay()).isEqualTo(2);
        assertThat(other.gameOfDay()).isEqualTo(1);
        // Newest day first, and within a day in playing order.
        assertThat(games.list()).extracting(GameDto::id).containsExactly(other.id(), first.id(), second.id());
    }

    @Test
    void movingAGameToAnotherDayRenumbersBothDays() {
        GameDto a = game(TUESDAY, 1L, 2L);
        GameDto b = game(TUESDAY, 1L, 2L);
        GameDto c = game(TUESDAY, 1L, 2L);
        game(WEDNESDAY, 1L, 2L);

        GameDto moved = games.update(b.id(), new UpdateGameRequest(WEDNESDAY, List.of(1L, 2L), null));

        assertThat(moved.gameOfDay()).isEqualTo(2);
        assertThat(moved.gamesThatDay()).isEqualTo(2);
        assertThat(games.get(a.id()).gameOfDay()).isEqualTo(1);
        assertThat(games.get(c.id()).gameOfDay()).isEqualTo(2);
        assertThat(games.get(c.id()).gamesThatDay()).isEqualTo(2);
    }

    @Test
    void deletingAGameClosesTheGapInItsDay() {
        GameDto a = game(TUESDAY, 1L, 2L);
        GameDto b = game(TUESDAY, 1L, 2L);
        GameDto c = game(TUESDAY, 1L, 2L);

        games.delete(b.id());

        assertThat(games.get(a.id()).gameOfDay()).isEqualTo(1);
        assertThat(games.get(c.id()).gameOfDay()).isEqualTo(2);
        assertThatThrownBy(() -> games.get(b.id())).isInstanceOf(NotFoundException.class);
    }

    @Test
    void playersAreFixedOnceThereAreRoundsButSeatingCanChange() {
        GameDto g = game(TUESDAY, 1L, 2L);
        games.addRound(g.id(), round(0, false));

        assertThatThrownBy(() -> games.update(g.id(), new UpdateGameRequest(TUESDAY, List.of(1L, 3L), null)))
                .isInstanceOf(ConflictException.class);

        GameDto reseated = games.update(g.id(), new UpdateGameRequest(TUESDAY, List.of(2L, 1L), "swapped"));
        assertThat(reseated.playerIds()).containsExactly(2L, 1L);
        assertThat(reseated.note()).isEqualTo("swapped");
    }

    @Test
    void changingPlayersBeforeAnyRoundDropsTheLeaversMoney() {
        GameDto g = game(TUESDAY, 1L, 2L);
        games.saveMoney(g.id(), List.of(new GameMoneyDto(1L, 100), new GameMoneyDto(2L, -100)));

        GameDto changed = games.update(g.id(), new UpdateGameRequest(TUESDAY, List.of(1L, 3L), null));

        assertThat(changed.playerIds()).containsExactly(1L, 3L);
        assertThat(changed.money()).extracting(GameMoneyDto::playerId).containsExactly(1L);
    }

    @Test
    void roundsNumberThemselvesAndCloseUpWhenOneIsDeleted() {
        GameDto g = game(TUESDAY, 1L, 2L);
        games.addRound(g.id(), round(0, false));
        GameDto withTwo = games.addRound(g.id(), round(0, false));
        GameDto withThree = games.addRound(g.id(), round(0, false));
        assertThat(withThree.rounds()).extracting(RoundDto::number).containsExactly(1, 2, 3);

        GameDto after = games.deleteRound(g.id(), withTwo.rounds().get(1).id());

        assertThat(after.rounds()).extracting(RoundDto::number).containsExactly(1, 2);
    }

    @Test
    void aMajstorskaClosesTheGameAndRemovingItReopens() {
        GameDto g = game(TUESDAY, 1L, 2L);
        games.addRound(g.id(), round(0, false));
        GameDto closed = games.addRound(g.id(), round(0, true));
        assertThat(closed.majstorska()).isTrue();
        assertThat(closed.inProgress()).isFalse();

        // Editing the Majstorska into a normal round reopens the game...
        RoundDto last = closed.rounds().getLast();
        GameDto edited = games.updateRound(g.id(), last.id(), round(last.number(), false));
        assertThat(edited.majstorska()).isFalse();
        assertThat(edited.inProgress()).isTrue();

        // ...and making it one again closes it.
        GameDto again = games.updateRound(g.id(), last.id(), round(last.number(), true));
        assertThat(again.inProgress()).isFalse();
        GameDto reopened = games.deleteRound(g.id(), last.id());
        assertThat(reopened.majstorska()).isFalse();
        assertThat(reopened.inProgress()).isTrue();
    }

    @Test
    void moneyIsReplacedAndAGameCanBeClosedByHand() {
        GameDto g = game(TUESDAY, 1L, 2L);
        games.saveMoney(g.id(), List.of(new GameMoneyDto(1L, 50), new GameMoneyDto(2L, -50)));
        GameDto saved = games.saveMoney(g.id(), List.of(new GameMoneyDto(1L, 300), new GameMoneyDto(2L, -300)));
        assertThat(saved.money()).extracting(GameMoneyDto::amountDen).containsExactly(300, -300);

        assertThat(games.setInProgress(g.id(), false).inProgress()).isFalse();
        assertThat(games.setInProgress(g.id(), true).inProgress()).isTrue();
    }

    @Test
    void anUnknownGameOrRoundIsNotFound() {
        GameDto g = game(TUESDAY, 1L, 2L);
        assertThatThrownBy(() -> games.get(-1L)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> games.deleteRound(g.id(), -1L)).isInstanceOf(NotFoundException.class);
    }
}
