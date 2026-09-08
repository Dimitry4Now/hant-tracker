package com.hanttracker.api.security;

import com.hanttracker.api.domain.UserAccount;
import com.hanttracker.api.domain.UserStatus;
import com.hanttracker.api.repo.UserAccountRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Turns a valid `Authorization: Bearer …` header into an authenticated request. */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String PREFIX = "Bearer ";

    private final JwtService jwt;
    private final UserAccountRepository users;

    public JwtAuthFilter(JwtService jwt, UserAccountRepository users) {
        this.jwt = jwt;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null
                && header.startsWith(PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            jwt.readUserId(header.substring(PREFIX.length()))
                    .flatMap(users::findById)
                    // A locked account keeps its token but stops being usable.
                    .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                    .ifPresent(this::authenticate);
        }

        chain.doFilter(request, response);
    }

    private void authenticate(UserAccount user) {
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        var authentication = UsernamePasswordAuthenticationToken.authenticated(user, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
