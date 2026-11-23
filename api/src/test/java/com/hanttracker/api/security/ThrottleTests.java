package com.hanttracker.api.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class ThrottleTests {

    /** A clock that moves only when told to. */
    static final class TestClock extends Clock {
        Instant now = Instant.parse("2026-11-23T10:00:00Z");

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }

    @Test
    void allowsTheLimitPerWindowThenWaitsForTheNext() {
        TestClock clock = new TestClock();
        Throttle throttle = new Throttle(2, Duration.ofMinutes(10), clock);

        assertThat(throttle.tryAcquire("a")).isTrue();
        assertThat(throttle.hasRoom("a")).isTrue();
        assertThat(throttle.tryAcquire("a")).isTrue();
        assertThat(throttle.hasRoom("a")).isFalse();
        assertThat(throttle.tryAcquire("a")).isFalse();
        assertThat(throttle.tryAcquire("b")).isTrue();

        clock.now = clock.now.plus(Duration.ofMinutes(10));
        assertThat(throttle.hasRoom("a")).isTrue();
        assertThat(throttle.tryAcquire("a")).isTrue();
    }
}
