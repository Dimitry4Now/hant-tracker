package com.hanttracker.api.repo;

import com.hanttracker.api.domain.Game;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface GameRepository extends JpaRepository<Game, Long>, JpaSpecificationExecutor<Game> {

    /** Newest day first; within a day, in the order they were played. */
    List<Game> findAllByOrderByPlayedOnDescGameOfDayAsc();
}
