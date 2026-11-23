package com.hanttracker.api.security;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * The limits on what anyone can try without an account, or too often with
 * one: guessing passwords, flooding sign-ups and emails. Addresses are the
 * client's, as nginx forwards them (server.forward-headers-strategy).
 */
@Component
public class RateLimits {

    private static final Clock CLOCK = Clock.systemUTC();

    /** Logins from one address, right or wrong. */
    public final Throttle loginPerAddress = new Throttle(20, Duration.ofMinutes(10), CLOCK);

    /** Wrong passwords for one email, from anywhere, so guessing is slow even spread out. */
    public final Throttle loginFailuresPerEmail = new Throttle(5, Duration.ofMinutes(15), CLOCK);

    public final Throttle registerPerAddress = new Throttle(5, Duration.ofHours(1), CLOCK);

    public final Throttle forgotPerAddress = new Throttle(5, Duration.ofHours(1), CLOCK);

    /** Reset emails to one address, so nobody is flooded with them. */
    public final Throttle forgotPerEmail = new Throttle(3, Duration.ofHours(1), CLOCK);

    public final Throttle resetPerAddress = new Throttle(10, Duration.ofHours(1), CLOCK);

    /** Confirmation codes one player can have sent. */
    public final Throttle emailCodePerUser = new Throttle(5, Duration.ofHours(1), CLOCK);

    /** Forgets every count, for tests. */
    public void clear() {
        List.of(
                        loginPerAddress,
                        loginFailuresPerEmail,
                        registerPerAddress,
                        forgotPerAddress,
                        forgotPerEmail,
                        resetPerAddress,
                        emailCodePerUser)
                .forEach(Throttle::clear);
    }
}
