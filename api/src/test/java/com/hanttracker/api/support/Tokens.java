package com.hanttracker.api.support;

import com.hanttracker.api.repo.UserAccountRepository;
import com.hanttracker.api.security.JwtService;
import org.springframework.stereotype.Component;

/** Login tokens for the dev seed's players, sent the way the app sends them. */
@Component
public class Tokens {

    public static final String ADMIN = "playera@example.com";
    public static final String PLAYER = "playerb@example.com";

    private final UserAccountRepository users;
    private final JwtService jwt;

    public Tokens(UserAccountRepository users, JwtService jwt) {
        this.users = users;
        this.jwt = jwt;
    }

    /** The Authorization header value for the seeded account with this email. */
    public String bearer(String email) {
        return "Bearer " + jwt.issue(users.findByEmailIgnoreCase(email).orElseThrow());
    }
}
