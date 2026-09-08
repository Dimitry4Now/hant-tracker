package com.hanttracker.api.web;

import com.hanttracker.api.dto.GameDto;
import com.hanttracker.api.dto.GameDtos.CreateGameRequest;
import com.hanttracker.api.dto.GameDtos.CreateRoundRequest;
import com.hanttracker.api.dto.GameDtos.FinishGameRequest;
import com.hanttracker.api.dto.GameDtos.SaveMoneyRequest;
import com.hanttracker.api.service.GameService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GameService games;

    public GameController(GameService games) {
        this.games = games;
    }

    @GetMapping
    public List<GameDto> list() {
        return games.list();
    }

    @GetMapping("/{id}")
    public GameDto get(@PathVariable Long id) {
        return games.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GameDto create(@Valid @RequestBody CreateGameRequest request) {
        return games.create(request);
    }

    @PostMapping("/{id}/rounds")
    @ResponseStatus(HttpStatus.CREATED)
    public GameDto addRound(@PathVariable Long id, @Valid @RequestBody CreateRoundRequest request) {
        return games.addRound(id, request);
    }

    @PutMapping("/{id}/money")
    public GameDto saveMoney(@PathVariable Long id, @Valid @RequestBody SaveMoneyRequest request) {
        return games.saveMoney(id, request.money());
    }

    @PutMapping("/{id}/status")
    public GameDto setStatus(@PathVariable Long id, @RequestBody FinishGameRequest request) {
        return games.setInProgress(id, request.inProgress());
    }
}
