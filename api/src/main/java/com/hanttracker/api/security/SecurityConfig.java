package com.hanttracker.api.security;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final AccountSetup accountSetup;

    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, AccountSetup accountSetup) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.accountSetup = accountSetup;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.requestMatchers(HttpMethod.OPTIONS, "/**")
                        .permitAll()
                        // Error dispatches are filtered too — without this every
                        // 403/409 comes back out as a 401.
                        .requestMatchers("/error")
                        .permitAll()
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/register",
                                "/api/auth/forgot-password",
                                "/api/auth/reset-password")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/options")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/stats/public")
                        .permitAll()
                        .requestMatchers("/api/account-requests/**")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/games/**")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/games/**")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/games/**")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/users/**")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/users/**")
                        .hasRole("ADMIN")
                        .anyRequest()
                        .authenticated())
                // 401 instead of a redirect to a login page the API does not serve.
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                // Not a bean, so it runs only here, once the token has been read.
                .addFilterAfter(new AccountSetupFilter(accountSetup), JwtAuthFilter.class)
                .build();
    }

    /**
     * Logins go through /api/auth and the JWT filter, not Spring's own login, so
     * there are no users here. Without it Spring Boot makes an in-memory user
     * with a generated password, and warns about it at every start.
     */
    @Bean
    public UserDetailsService noSpringLogins() {
        return username -> {
            throw new UsernameNotFoundException(username);
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
