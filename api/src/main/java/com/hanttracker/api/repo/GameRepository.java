package com.hanttracker.api.repo;

import com.hanttracker.api.domain.Game;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRepository extends JpaRepository<Game, Long> {

    /** Newest day first; within a day, in the order they were played. */
    List<Game> findAllByOrderByPlayedOnDescGameOfDayAsc();

    List<Game> findByPlayedOn(LocalDate playedOn);

    long countByPlayedOn(LocalDate playedOn);
}
