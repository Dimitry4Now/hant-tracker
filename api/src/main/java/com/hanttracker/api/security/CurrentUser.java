package com.hanttracker.api.security;

import com.hanttracker.api.domain.UserAccount;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Reads the {@link UserAccount} the {@link JwtAuthFilter} put in the context. */
public final class CurrentUser {

    private CurrentUser() {}

    public static Optional<UserAccount> get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserAccount user)) {
            return Optional.empty();
        }
        return Optional.of(user);
    }

    public static UserAccount require() {
        return get().orElseThrow(() -> new IllegalStateException("No authenticated user"));
    }
}
