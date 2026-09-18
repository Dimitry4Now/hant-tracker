package com.hanttracker.api.service;

import com.hanttracker.api.domain.Game;
import com.hanttracker.api.domain.Round;
import com.hanttracker.api.dto.GameDto;
import com.hanttracker.api.dto.GameDtos.CreateGameRequest;
import com.hanttracker.api.dto.GameDtos.CreateRoundRequest;
import com.hanttracker.api.dto.GameDtos.UpdateGameRequest;
import com.hanttracker.api.dto.GameMoneyDto;
import com.hanttracker.api.dto.RoundEntryDto;
import com.hanttracker.api.repo.GameRepository;
import com.hanttracker.api.web.ConflictException;
import com.hanttracker.api.web.NotFoundException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class GameService {

    private final GameRepository games;

    public GameService(GameRepository games) {
        this.games = games;
    }

    @Transactional(readOnly = true)
    public List<GameDto> list() {
        List<Game> all = games.findAllByOrderByPlayedOnDescGameOfDayAsc();
        Map<LocalDate, Long> perDay =
                all.stream().collect(Collectors.groupingBy(Game::getPlayedOn, Collectors.counting()));
        return all.stream()
                .map(game -> GameDto.of(game, perDay.get(game.getPlayedOn()).intValue()))
                .toList();
    }

    @Transactional(readOnly = true)
    public GameDto get(Long id) {
        Game game = find(id);
        return GameDto.of(game, (int) games.countByPlayedOn(game.getPlayedOn()));
    }

    public GameDto create(CreateGameRequest request) {
        List<Game> sameDay = games.findByPlayedOn(request.playedOn());

        Game game = new Game();
        game.setPlayedOn(request.playedOn());
        game.setGameOfDay(sameDay.size() + 1);
        game.setInProgress(true);
        game.setNote(request.note());
        game.setPlayerIds(new ArrayList<>(request.playerIds()));

        games.save(game);
        return GameDto.of(game, sameDay.size() + 1);
    }

    /**
     * Points arrive already computed by the client, which owns the scoring rules.
     * A Majstorska round is the last one, so it also closes the game.
     */
    public GameDto addRound(Long gameId, CreateRoundRequest request) {
        Game game = find(gameId);

        Round round = new Round();
        round.setNumber(request.number() > 0 ? request.number() : game.getRounds().size() + 1);
        round.setDealerId(request.dealerId());
        round.setMajstorska(request.majstorska());
        round.setHant(request.hant());
        round.setComment(request.comment());
        round.setEntries(
                request.entries().stream()
                        .map(RoundEntryDto::toEntity)
                        .collect(Collectors.toCollection(ArrayList::new)));
        game.addRound(round);

        if (request.majstorska()) {
            game.setMajstorska(true);
            game.setInProgress(false);
        }

        return GameDto.of(game, (int) games.countByPlayedOn(game.getPlayedOn()));
    }

    /**
     * A new date moves the game to the end of that day and closes the gap it
     * left behind, so "Game N of the day" stays 1..n on both dates.
     */
    public GameDto update(Long gameId, UpdateGameRequest request) {
        Game game = find(gameId);

        if (!game.getPlayerIds().equals(request.playerIds())) {
            boolean samePlayers = new HashSet<>(game.getPlayerIds()).equals(new HashSet<>(request.playerIds()))
                    && game.getPlayerIds().size() == request.playerIds().size();
            if (!samePlayers && !game.getRounds().isEmpty()) {
                throw new ConflictException("Players cannot change once the game has rounds");
            }
            // Only the seating order changes when the players are the same.
            game.getPlayerIds().clear();
            game.getPlayerIds().addAll(request.playerIds());
            game.getMoney().removeIf(entry -> !request.playerIds().contains(entry.getPlayerId()));
        }

        LocalDate previousDate = game.getPlayedOn();
        if (!previousDate.equals(request.playedOn())) {
            game.setGameOfDay(games.findByPlayedOn(request.playedOn()).size() + 1);
            game.setPlayedOn(request.playedOn());
            games.flush();
            renumberDay(previousDate);
        }

        game.setNote(request.note());
        return GameDto.of(game, (int) games.countByPlayedOn(game.getPlayedOn()));
    }

    public void delete(Long gameId) {
        Game game = find(gameId);
        games.delete(game);
        games.flush();
        renumberDay(game.getPlayedOn());
    }

    /** Same rules as adding one; the points arrive recomputed by the client. */
    public GameDto updateRound(Long gameId, Long roundId, CreateRoundRequest request) {
        Game game = find(gameId);
        Round round = findRound(game, roundId);

        round.setNumber(request.number() > 0 ? request.number() : round.getNumber());
        round.setDealerId(request.dealerId());
        round.setMajstorska(request.majstorska());
        round.setHant(request.hant());
        round.setComment(request.comment());
        round.getEntries().clear();
        request.entries().stream().map(RoundEntryDto::toEntity).forEach(round.getEntries()::add);

        syncMajstorska(game);
        return GameDto.of(game, (int) games.countByPlayedOn(game.getPlayedOn()));
    }

    /** Later rounds move up one, so the numbers stay 1..n. */
    public GameDto deleteRound(Long gameId, Long roundId) {
        Game game = find(gameId);
        game.getRounds().remove(findRound(game, roundId));

        List<Round> remaining = new ArrayList<>(game.getRounds());
        remaining.sort(Comparator.comparingInt(Round::getNumber));
        for (int i = 0; i < remaining.size(); i++) {
            remaining.get(i).setNumber(i + 1);
        }
        syncMajstorska(game);
        return GameDto.of(game, (int) games.countByPlayedOn(game.getPlayedOn()));
    }

    public GameDto saveMoney(Long gameId, List<GameMoneyDto> money) {
        Game game = find(gameId);
        game.setMoney(money.stream().map(GameMoneyDto::toEntity).collect(Collectors.toCollection(ArrayList::new)));
        return GameDto.of(game, (int) games.countByPlayedOn(game.getPlayedOn()));
    }

    public GameDto setInProgress(Long gameId, boolean inProgress) {
        Game game = find(gameId);
        game.setInProgress(inProgress);
        return GameDto.of(game, (int) games.countByPlayedOn(game.getPlayedOn()));
    }

    /**
     * The game's Majstorska flag follows its rounds. Recording one closes the
     * game; editing or deleting the last one away reopens it.
     */
    private void syncMajstorska(Game game) {
        boolean hasMajstorska = game.getRounds().stream().anyMatch(Round::isMajstorska);
        if (hasMajstorska && !game.isMajstorska()) {
            game.setInProgress(false);
        } else if (!hasMajstorska && game.isMajstorska()) {
            game.setInProgress(true);
        }
        game.setMajstorska(hasMajstorska);
    }

    private void renumberDay(LocalDate day) {
        List<Game> sameDay = new ArrayList<>(games.findByPlayedOn(day));
        sameDay.sort(Comparator.comparingInt(Game::getGameOfDay));
        for (int i = 0; i < sameDay.size(); i++) {
            sameDay.get(i).setGameOfDay(i + 1);
        }
    }

    private Round findRound(Game game, Long roundId) {
        return game.getRounds().stream()
                .filter(round -> round.getId().equals(roundId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("No round with id " + roundId + " in this game"));
    }

    private Game find(Long id) {
        return games.findById(id)
                .orElseThrow(() -> new NotFoundException("No game with id " + id));
    }

}
