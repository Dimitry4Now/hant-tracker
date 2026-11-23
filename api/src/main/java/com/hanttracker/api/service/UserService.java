package com.hanttracker.api.service;

import com.hanttracker.api.domain.AccountRequest;
import com.hanttracker.api.domain.UserAccount;
import com.hanttracker.api.domain.UserRole;
import com.hanttracker.api.domain.UserStatus;
import com.hanttracker.api.dto.AccountDtos.ChangePasswordRequest;
import com.hanttracker.api.dto.AccountDtos.UpdateProfileRequest;
import com.hanttracker.api.dto.AccountRequestDto;
import com.hanttracker.api.dto.AuthDtos.RegisterRequest;
import com.hanttracker.api.dto.CreatedUserDto;
import com.hanttracker.api.dto.TemporaryPasswordDto;
import com.hanttracker.api.dto.UserDto;
import com.hanttracker.api.dto.UserDtos.CreateUserRequest;
import com.hanttracker.api.repo.AccountRequestRepository;
import com.hanttracker.api.repo.UserAccountRepository;
import com.hanttracker.api.security.Secrets;
import com.hanttracker.api.web.ConflictException;
import com.hanttracker.api.web.NotFoundException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    /** Sign-ups waiting for an admin; more than this is someone flooding the form. */
    static final int MAX_PENDING_REQUESTS = 50;

    private final UserAccountRepository users;
    private final AccountRequestRepository requests;
    private final PasswordEncoder encoder;
    private final String defaultPassword;

    public UserService(
            UserAccountRepository users,
            AccountRequestRepository requests,
            PasswordEncoder encoder,
            @Value("${app.default-password}") String defaultPassword) {
        this.users = users;
        this.requests = requests;
        this.encoder = encoder;
        this.defaultPassword = defaultPassword;
    }

    /** Admins see everything; players only what they need to show names. */
    @Transactional(readOnly = true)
    public List<UserDto> list(boolean admin) {
        return users.findAllByOrderByIdAsc().stream()
                .map(admin ? UserDto::of : UserDto::forPlayers)
                .toList();
    }

    /**
     * Without a password from the admin, the player gets a random one for the
     * admin to pass on. Either way they replace it at their first login.
     */
    public CreatedUserDto create(CreateUserRequest request) {
        requireEmailFree(request.email());
        String temporary = StringUtils.hasText(request.password()) ? null : Secrets.temporaryPassword();

        UserAccount user = new UserAccount();
        user.setFullName(request.fullName());
        user.setDisplayName(request.displayName());
        user.setEmail(request.email());
        user.setRole(request.role());
        user.setStatus(UserStatus.ACTIVE);
        user.setJoinedOn(LocalDate.now());
        user.setPasswordHash(encoder.encode(temporary != null ? temporary : request.password()));
        user.setMustChangePassword(true);

        return new CreatedUserDto(UserDto.of(users.save(user)), temporary);
    }

    /**
     * Gives a player a random one-time password, for when they forgot theirs.
     * It signs them out everywhere, and they replace it at their next login.
     */
    public TemporaryPasswordDto resetPassword(Long id) {
        UserAccount user = findUser(id);
        String temporary = Secrets.temporaryPassword();
        user.setPasswordHash(encoder.encode(temporary));
        user.setMustChangePassword(true);
        user.setTokenVersion(user.getTokenVersion() + 1);
        return new TemporaryPasswordDto(temporary);
    }

    public UserDto setStatus(Long id, UserStatus status) {
        UserAccount user = findUser(id);
        user.setStatus(status);
        return UserDto.of(user);
    }

    public UserDto rename(Long id, String displayName) {
        UserAccount user = findUser(id);
        user.setDisplayName(displayName);
        return UserDto.of(user);
    }

    /** Admin-side, so no password check — the admin is fixing someone else's account. */
    public UserDto changeEmail(Long id, String email) {
        UserAccount user = findUser(id);
        String trimmed = email.trim();
        if (!trimmed.equalsIgnoreCase(user.getEmail())) {
            requireEmailFree(trimmed);
        }
        user.setEmail(trimmed);
        return UserDto.of(user);
    }

    // --- own account ----------------------------------------------------

    /** Changing the email needs the current password; the display name does not. */
    public UserDto updateProfile(Long id, UpdateProfileRequest request) {
        UserAccount user = findUser(id);
        String email = request.email().trim();

        if (!email.equalsIgnoreCase(user.getEmail())) {
            requireCurrentPassword(user, request.currentPassword());
            requireEmailFree(email);
        }

        user.setDisplayName(request.displayName().trim());
        user.setEmail(email);
        return UserDto.of(user);
    }

    /**
     * Replaces the player's password and signs out their other sessions; the
     * caller gets a fresh token for this one.
     */
    public UserAccount changePassword(Long id, ChangePasswordRequest request) {
        UserAccount user = findUser(id);
        requireCurrentPassword(user, request.currentPassword());
        if (request.newPassword().equals(request.currentPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pick a new password, not the current one");
        }
        setPassword(user, request.newPassword());
        return user;
    }

    /** Sets a password the player chose, which clears the need to change it. */
    void setPassword(UserAccount user, String password) {
        requireUsablePassword(password);
        user.setPasswordHash(encoder.encode(password));
        user.setMustChangePassword(false);
        user.setTokenVersion(user.getTokenVersion() + 1);
    }

    /** BCrypt reads only the first 72 bytes, and the old default password is public. */
    void requireUsablePassword(String password) {
        if (password.equals(defaultPassword)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pick a password other than the default one");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "That password is too long");
        }
    }

    // --- sign-up requests -----------------------------------------------

    @Transactional(readOnly = true)
    public List<AccountRequestDto> listRequests() {
        return requests.findAllByOrderByRequestedOnDesc().stream().map(AccountRequestDto::of).toList();
    }

    /**
     * Stores a pending sign-up with its hashed password, for an admin to approve.
     * A taken email, or a queue already full of requests, is quietly dropped:
     * telling the visitor would tell anyone which emails have accounts.
     */
    public void register(RegisterRequest request) {
        if (emailTaken(request.email())) {
            log.info("Sign-up for an email that is already taken; ignored");
            return;
        }
        if (requests.count() >= MAX_PENDING_REQUESTS) {
            log.warn("Sign-up ignored: {} requests are already waiting", MAX_PENDING_REQUESTS);
            return;
        }
        requireUsablePassword(request.password());

        AccountRequest pending = new AccountRequest();
        pending.setFullName(request.fullName());
        pending.setDisplayName(request.displayName());
        pending.setEmail(request.email());
        pending.setPasswordHash(encoder.encode(request.password()));
        pending.setRequestedOn(LocalDate.now());
        requests.save(pending);
    }

    public UserDto approveRequest(Long id) {
        AccountRequest pending =
                requests.findById(id).orElseThrow(() -> new NotFoundException("No request with id " + id));

        UserAccount user = new UserAccount();
        user.setFullName(pending.getFullName());
        user.setDisplayName(pending.getDisplayName());
        user.setEmail(pending.getEmail());
        user.setPasswordHash(pending.getPasswordHash());
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setJoinedOn(LocalDate.now());

        users.save(user);
        requests.delete(pending);
        return UserDto.of(user);
    }

    public void rejectRequest(Long id) {
        if (!requests.existsById(id)) {
            throw new NotFoundException("No request with id " + id);
        }
        requests.deleteById(id);
    }

    private UserAccount findUser(Long id) {
        return users.findById(id).orElseThrow(() -> new NotFoundException("No user with id " + id));
    }

    /**
     * 400 rather than 401/403: the frontend treats those as an expired session
     * and logs the user out.
     */
    private void requireCurrentPassword(UserAccount user, String password) {
        if (!StringUtils.hasText(password) || !encoder.matches(password, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is wrong");
        }
    }

    private void requireEmailFree(String email) {
        if (emailTaken(email)) {
            throw new ConflictException("That email is already taken");
        }
    }

    private boolean emailTaken(String email) {
        return users.existsByEmailIgnoreCase(email) || requests.existsByEmailIgnoreCase(email);
    }
}
