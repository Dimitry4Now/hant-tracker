package com.hanttracker.api.repo.spec;

import java.time.LocalDate;
import java.time.YearMonth;
import org.springframework.data.jpa.domain.Specification;

/**
 * Date filters shared by every entity with a {@code playedOn} date: games,
 * and the per-game stats views.
 */
public final class PlayedOnSpecs {

    private static final String PLAYED_ON = "playedOn";

    private PlayedOnSpecs() {}

    public static <T> Specification<T> playedOn(LocalDate day) {
        return (root, query, cb) -> cb.equal(root.get(PLAYED_ON), day);
    }

    /** Both ends included. */
    public static <T> Specification<T> playedBetween(LocalDate from, LocalDate to) {
        return (root, query, cb) -> cb.between(root.get(PLAYED_ON), from, to);
    }

    public static <T> Specification<T> playedIn(YearMonth month) {
        return playedBetween(month.atDay(1), month.atEndOfMonth());
    }

    public static <T> Specification<T> finished() {
        return (root, query, cb) -> cb.isFalse(root.get("inProgress"));
    }
}
