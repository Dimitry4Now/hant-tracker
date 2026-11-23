package com.hanttracker.api.service;

import com.hanttracker.api.domain.EmailCode;
import com.hanttracker.api.domain.PasswordReset;
import com.hanttracker.api.domain.UserAccount;
import com.hanttracker.api.domain.UserStatus;
import com.hanttracker.api.repo.AccountRequestRepository;
import com.hanttracker.api.repo.EmailCodeRepository;
import com.hanttracker.api.repo.PasswordResetRepository;
import com.hanttracker.api.repo.UserAccountRepository;
import com.hanttracker.api.security.RateLimits;
import com.hanttracker.api.security.Secrets;
import com.hanttracker.api.web.ConflictException;
import com.hanttracker.api.web.NotFoundException;
import com.hanttracker.api.web.TooManyRequestsException;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Confirming an email with a code, and resetting a forgotten password with a
 * link sent to a confirmed email.
 */
@Service
@Transactional
public class EmailAccountService {

    private static final Logger log = LoggerFactory.getLogger(EmailAccountService.class);

    static final Duration CODE_LIFETIME = Duration.ofMinutes(15);
    static final int CODE_ATTEMPTS = 5;
    static final Duration RESET_LIFETIME = Duration.ofHours(1);

    private final UserAccountRepository users;
    private final AccountRequestRepository requests;
    private final EmailCodeRepository codes;
    private final PasswordResetRepository resets;
    private final UserService userService;
    private final Mailer mailer;
    private final RateLimits limits;
    private final String publicUrl;

    public EmailAccountService(
            UserAccountRepository users,
            AccountRequestRepository requests,
            EmailCodeRepository codes,
            PasswordResetRepository resets,
            UserService userService,
            Mailer mailer,
            RateLimits limits,
            @Value("${app.public-url}") String publicUrl) {
        this.users = users;
        this.requests = requests;
        this.codes = codes;
        this.resets = resets;
        this.userService = userService;
        this.mailer = mailer;
        this.limits = limits;
        this.publicUrl = publicUrl.replaceAll("/+$", "");
    }

    // --- confirming an email ---------------------------------------------

    /** Emails a code to {@code email}; it becomes the player's once they type it in. */
    public void sendCode(Long userId, String email) {
        requireMail();
        UserAccount user = findUser(userId);
        String address = email.trim();
        requireFreeFor(user, address);
        if (!limits.emailCodePerUser.tryAcquire(String.valueOf(userId))) {
            throw new TooManyRequestsException();
        }

        String code = Secrets.code();
        EmailCode pending = codes.findById(userId).orElseGet(EmailCode::new);
        pending.setUserId(userId);
        pending.setEmail(address);
        pending.setCodeHash(Secrets.sha256(code));
        pending.setExpiresAt(Instant.now().plus(CODE_LIFETIME));
        pending.setAttempts(0);
        codes.save(pending);

        mailer.send(address, "Your Hant Tracker code", """
                Your code is %s.

                Type it into Hant Tracker to confirm this email. It works for %d minutes.

                If you didn't ask for it, ignore this email.
                """.formatted(code, CODE_LIFETIME.toMinutes()));
    }

    /**
     * Confirms the email the code went to. A wrong code counts against the
     * code's few attempts, so that is kept even though the call fails.
     */
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public UserAccount confirmCode(Long userId, String code) {
        UserAccount user = findUser(userId);
        EmailCode pending = codes.findById(userId)
                .filter(c -> c.getExpiresAt().isAfter(Instant.now()) && c.getAttempts() < CODE_ATTEMPTS)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "That code has run out. Ask for a new one."));

        if (!Secrets.sameHash(Secrets.sha256(code.trim()), pending.getCodeHash())) {
            pending.setAttempts(pending.getAttempts() + 1);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "That code is wrong");
        }

        requireFreeFor(user, pending.getEmail());
        user.setEmail(pending.getEmail());
        user.setEmailVerified(true);
        codes.delete(pending);
        return user;
    }

    // --- resetting a forgotten password ----------------------------------

    /**
     * Emails a reset link if {@code email} is an active account's confirmed
     * address. The caller can't tell whether it was, so the form says the same
     * either way.
     */
    public void sendResetLink(String email, String address) {
        requireMail();
        if (!limits.forgotPerAddress.tryAcquire(address)) {
            throw new TooManyRequestsException();
        }
        String key = email.trim().toLowerCase(Locale.ROOT);
        if (!limits.forgotPerEmail.tryAcquire(key)) {
            return;
        }
        Optional<UserAccount> found = users.findByEmailIgnoreCase(email.trim())
                .filter(UserAccount::isEmailVerified)
                .filter(user -> user.getStatus() == UserStatus.ACTIVE);
        if (found.isEmpty()) {
            return;
        }
        UserAccount user = found.get();

        String token = Secrets.token();
        resets.deleteByUserId(user.getId());
        PasswordReset reset = new PasswordReset();
        reset.setTokenHash(Secrets.sha256(token));
        reset.setUserId(user.getId());
        reset.setExpiresAt(Instant.now().plus(RESET_LIFETIME));
        resets.save(reset);

        try {
            mailer.send(user.getEmail(), "Reset your Hant Tracker password", """
                    Open this link to choose a new password:

                    %s/reset-password?token=%s

                    It works once, for an hour. If you didn't ask for it, ignore this email; \
                    your password stays as it is.
                    """.formatted(publicUrl, token));
        } catch (ResponseStatusException e) {
            // Failing here would tell the caller the account exists; the error is logged.
            log.warn("Reset link for user {} not sent", user.getId());
        }
    }

    /** Sets a new password from a reset link, which then stops working. */
    public UserAccount resetPassword(String token, String newPassword, String address) {
        if (!limits.resetPerAddress.tryAcquire(address)) {
            throw new TooManyRequestsException();
        }
        PasswordReset reset = resets.findById(Secrets.sha256(token.trim()))
                .filter(r -> r.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "This link has run out or was already used. Ask for a new one."));
        UserAccount user = findUser(reset.getUserId());
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This account is locked");
        }
        userService.setPassword(user, newPassword);
        resets.deleteByUserId(user.getId());
        return user;
    }

    private void requireMail() {
        if (!mailer.enabled()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Email is not set up");
        }
    }

    /** Another account, or a sign-up waiting for approval, may already have the address. */
    private void requireFreeFor(UserAccount user, String email) {
        boolean takenByOther = users.findByEmailIgnoreCase(email)
                .filter(other -> !other.getId().equals(user.getId()))
                .isPresent();
        if (takenByOther || requests.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("That email belongs to another account");
        }
    }

    private UserAccount findUser(Long id) {
        return users.findById(id).orElseThrow(() -> new NotFoundException("No user with id " + id));
    }
}
