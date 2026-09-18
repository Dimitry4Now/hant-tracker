package com.hanttracker.api.service;

import com.hanttracker.api.domain.AccountRequest;
import com.hanttracker.api.domain.UserAccount;
import com.hanttracker.api.domain.UserRole;
import com.hanttracker.api.domain.UserStatus;
import com.hanttracker.api.dto.AccountDtos.ChangePasswordRequest;
import com.hanttracker.api.dto.AccountDtos.UpdateProfileRequest;
import com.hanttracker.api.dto.AccountRequestDto;
import com.hanttracker.api.dto.AuthDtos.RegisterRequest;
import com.hanttracker.api.dto.UserDto;
import com.hanttracker.api.dto.UserDtos.CreateUserRequest;
import com.hanttracker.api.repo.AccountRequestRepository;
import com.hanttracker.api.repo.UserAccountRepository;
import com.hanttracker.api.web.ConflictException;
import com.hanttracker.api.web.NotFoundException;
import java.time.LocalDate;
import java.util.List;
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

    @Transactional(readOnly = true)
    public List<UserDto> list() {
        return users.findAllByOrderByIdAsc().stream().map(UserDto::of).toList();
    }

    public UserDto create(CreateUserRequest request) {
        requireEmailFree(request.email());

        UserAccount user = new UserAccount();
        user.setFullName(request.fullName());
        user.setDisplayName(request.displayName());
        user.setEmail(request.email());
        user.setRole(request.role());
        user.setStatus(UserStatus.ACTIVE);
        user.setJoinedOn(LocalDate.now());
        user.setPasswordHash(
                encoder.encode(
                        StringUtils.hasText(request.password()) ? request.password() : defaultPassword));

        return UserDto.of(users.save(user));
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

    public void changePassword(Long id, ChangePasswordRequest request) {
        UserAccount user = findUser(id);
        requireCurrentPassword(user, request.currentPassword());
        user.setPasswordHash(encoder.encode(request.newPassword()));
    }

    // --- sign-up requests -----------------------------------------------

    @Transactional(readOnly = true)
    public List<AccountRequestDto> listRequests() {
        return requests.findAllByOrderByRequestedOnDesc().stream().map(AccountRequestDto::of).toList();
    }

    /** Stores a pending sign-up with its hashed password, for an admin to approve. */
    public AccountRequestDto register(RegisterRequest request) {
        requireEmailFree(request.email());

        AccountRequest pending = new AccountRequest();
        pending.setFullName(request.fullName());
        pending.setDisplayName(request.displayName());
        pending.setEmail(request.email());
        pending.setPasswordHash(encoder.encode(request.password()));
        pending.setRequestedOn(LocalDate.now());

        return AccountRequestDto.of(requests.save(pending));
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
        if (users.existsByEmailIgnoreCase(email) || requests.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("That email is already taken");
        }
    }
}
