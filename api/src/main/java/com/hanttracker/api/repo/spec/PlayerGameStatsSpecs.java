package com.hanttracker.api.repo.spec;

import com.hanttracker.api.domain.PlayerGameStats;
import org.springframework.data.jpa.domain.Specification;

/**
 * Filters over {@link PlayerGameStats}, meant to be chained:
 *
 * <pre>{@code
 * stats.findAll(forPlayer(id).and(playedIn(YearMonth.now())).and(finished()));
 * }</pre>
 *
 * Date and finished filters come from {@link PlayedOnSpecs}.
 */
public final class PlayerGameStatsSpecs {

    private PlayerGameStatsSpecs() {}

    public static Specification<PlayerGameStats> forPlayer(Long playerId) {
        return (root, query, cb) -> cb.equal(root.get("playerId"), playerId);
    }
}
