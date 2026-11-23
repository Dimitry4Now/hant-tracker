package com.hanttracker.api.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Allows up to {@code limit} events per key in each {@code window}, counted in
 * a fixed window that starts with a key's first event. Kept in memory: the API
 * runs as one process, and a restart forgetting the counts does no harm.
 */
public final class Throttle {

    /** Past this many keys, windows that have run out are dropped. */
    private static final int SWEEP_AT = 10_000;

    private final int limit;
    private final Duration window;
    private final Clock clock;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    private record Window(Instant start, int count) {}

    public Throttle(int limit, Duration window, Clock clock) {
        this.limit = limit;
        this.window = window;
        this.clock = clock;
    }

    /** Counts one event for {@code key}; false when its window is used up. */
    public boolean tryAcquire(String key) {
        Instant now = clock.instant();
        if (windows.size() >= SWEEP_AT) {
            windows.values().removeIf(w -> over(w, now));
        }
        boolean[] allowed = {false};
        windows.compute(key, (k, w) -> {
            if (w == null || over(w, now)) {
                allowed[0] = true;
                return new Window(now, 1);
            }
            if (w.count() < limit) {
                allowed[0] = true;
                return new Window(w.start(), w.count() + 1);
            }
            return w;
        });
        return allowed[0];
    }

    /** Whether {@code key} has events left, without counting one. */
    public boolean hasRoom(String key) {
        Window w = windows.get(key);
        return w == null || over(w, clock.instant()) || w.count() < limit;
    }

    public void clear() {
        windows.clear();
    }

    private boolean over(Window w, Instant now) {
        return !now.isBefore(w.start().plus(window));
    }
}
