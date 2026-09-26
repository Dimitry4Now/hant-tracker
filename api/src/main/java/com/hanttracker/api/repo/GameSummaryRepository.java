package com.hanttracker.api.repo;

import com.hanttracker.api.domain.GameSummary;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.Repository;

/** Read-only: the rows come from a view. Query it with {@code repo.spec} specifications. */
public interface GameSummaryRepository
        extends Repository<GameSummary, Long>, JpaSpecificationExecutor<GameSummary> {}
