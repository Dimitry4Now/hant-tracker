package com.hanttracker.api.service;

import com.hanttracker.api.domain.Game;
import com.hanttracker.api.domain.Round;
import com.hanttracker.api.dto.GameDto;
import com.hanttracker.api.dto.GameDtos.CreateGameRequest;
import com.hanttracker.api.dto.GameDtos.CreateRoundRequest;
import com.hanttracker.api.dto.GameMoneyDto;
import com.hanttracker.api.dto.RoundEntryDto;
import com.hanttracker.api.repo.GameRepository;
import com.hanttracker.api.web.NotFoundException;
import java.time.LocalDate;
import java.util.ArrayList;
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

    private Game find(Long id) {
        return games.findById(id)
                .orElseThrow(() -> new NotFoundException("No game with id " + id));
    }

}
