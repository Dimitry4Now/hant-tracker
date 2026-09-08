package com.hanttracker.api.seed;

import com.hanttracker.api.domain.UserAccount;
import com.hanttracker.api.domain.UserRole;
import com.hanttracker.api.domain.UserStatus;
import com.hanttracker.api.repo.UserAccountRepository;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Creates the first admin account on an otherwise empty database, so a fresh
 * production install has someone who can approve sign-ups. Does nothing once
 * any user exists — including after the credentials are removed from the
 * environment, which is what should happen after the first start.
 */
@Component
public class BootstrapAdmin implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdmin.class);

    private final UserAccountRepository users;
    private final PasswordEncoder encoder;
    private final String email;
    private final String password;
    private final String name;

    public BootstrapAdmin(
            UserAccountRepository users,
            PasswordEncoder encoder,
            @Value("${app.bootstrap-admin.email:}") String email,
            @Value("${app.bootstrap-admin.password:}") String password,
            @Value("${app.bootstrap-admin.name:Admin}") String name) {
        this.users = users;
        this.encoder = encoder;
        this.email = email;
        this.password = password;
        this.name = name;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!StringUtils.hasText(email) || !StringUtils.hasText(password) || users.count() > 0) {
            return;
        }

        UserAccount admin = new UserAccount();
        admin.setFullName(name);
        admin.setDisplayName(name);
        admin.setEmail(email.trim());
        admin.setPasswordHash(encoder.encode(password));
        admin.setRole(UserRole.ADMIN);
        admin.setStatus(UserStatus.ACTIVE);
        admin.setJoinedOn(LocalDate.now());
        users.save(admin);

        log.info("Created the bootstrap admin account for {}", admin.getEmail());
    }
}
