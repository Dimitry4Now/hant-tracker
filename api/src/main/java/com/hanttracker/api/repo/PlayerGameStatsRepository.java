package com.hanttracker.api.repo;

import com.hanttracker.api.domain.PlayerGameStats;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.Repository;

/** Read-only: the rows come from a view. Query it with {@code repo.spec} specifications. */
public interface PlayerGameStatsRepository
        extends Repository<PlayerGameStats, PlayerGameStats.Key>, JpaSpecificationExecutor<PlayerGameStats> {}
