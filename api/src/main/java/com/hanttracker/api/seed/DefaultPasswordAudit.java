package com.hanttracker.api.seed;

import com.hanttracker.api.domain.UserAccount;
import com.hanttracker.api.repo.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Finds accounts still on the old default password at startup and makes them
 * pick a new one at their next login. Admins see them flagged in the users
 * list, and can reset them so nobody else logs in with the default first.
 */
@Component
public class DefaultPasswordAudit implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DefaultPasswordAudit.class);

    private final UserAccountRepository users;
    private final PasswordEncoder encoder;
    private final String defaultPassword;

    public DefaultPasswordAudit(
            UserAccountRepository users,
            PasswordEncoder encoder,
            @Value("${app.default-password}") String defaultPassword) {
        this.users = users;
        this.encoder = encoder;
        this.defaultPassword = defaultPassword;
    }

    @Override
    @Transactional
    public void run(String... args) {
        int flagged = 0;
        for (UserAccount user : users.findAll()) {
            if (!user.isMustChangePassword() && encoder.matches(defaultPassword, user.getPasswordHash())) {
                user.setMustChangePassword(true);
                flagged++;
            }
        }
        if (flagged > 0) {
            log.warn("{} account(s) still use the default password; they must change it at their next login", flagged);
        }
    }
}
