package com.hanttracker.api.security;

import com.hanttracker.api.dto.RequiredStep;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Holds back a player who has a required step (a password to change, an email
 * to confirm) from
 * everything but that step: the API answers 428 Precondition Required, which
 * the app takes as "show the step", not as an expired session.
 */
public class AccountSetupFilter extends OncePerRequestFilter {

    /** What a player with a required step can still do. */
    private static final Set<String> ALLOWED = Set.of(
            "GET /api/auth/me",
            "GET /api/auth/options",
            "PUT /api/account/password",
            "POST /api/account/email",
            "POST /api/account/email/confirm");

    private final AccountSetup setup;

    public AccountSetupFilter(AccountSetup setup) {
        this.setup = setup;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        RequiredStep step = CurrentUser.get().map(setup::requiredStep).orElse(null);
        if (step != null && !ALLOWED.contains(request.getMethod() + " " + request.getRequestURI())) {
            response.setStatus(HttpStatus.PRECONDITION_REQUIRED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"requiredStep\":\"" + step + "\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
