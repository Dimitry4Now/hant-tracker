package com.hanttracker.api.web;

import com.hanttracker.api.domain.UserAccount;
import com.hanttracker.api.domain.UserStatus;
import com.hanttracker.api.dto.AuthDtos.LoginRequest;
import com.hanttracker.api.dto.AuthDtos.LoginResponse;
import com.hanttracker.api.dto.AuthDtos.RegisterRequest;
import com.hanttracker.api.dto.UserDto;
import com.hanttracker.api.repo.UserAccountRepository;
import com.hanttracker.api.security.CurrentUser;
import com.hanttracker.api.security.JwtService;
import com.hanttracker.api.security.RateLimits;
import com.hanttracker.api.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserAccountRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final UserService userService;
    private final RateLimits limits;
    /** Checked for unknown emails, so they take as long as a wrong password. */
    private final String unknownUserHash;

    public AuthController(
            UserAccountRepository users,
            PasswordEncoder encoder,
            JwtService jwt,
            UserService userService,
            RateLimits limits) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
        this.userService = userService;
        this.limits = limits;
        this.unknownUserHash = encoder.encode(UUID.randomUUID().toString());
    }

    /**
     * Unknown emails and wrong passwords get the same answer in the same time,
     * so a login can't tell which emails have accounts.
     */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        String email = request.email().trim();
        String emailKey = email.toLowerCase(Locale.ROOT);
        if (!limits.loginPerAddress.tryAcquire(http.getRemoteAddr())
                || !limits.loginFailuresPerEmail.hasRoom(emailKey)) {
            throw new TooManyRequestsException();
        }

        Optional<UserAccount> found = users.findByEmailIgnoreCase(email);
        boolean matches = encoder.matches(
                request.password(), found.map(UserAccount::getPasswordHash).orElse(unknownUserHash));
        if (found.isEmpty() || !matches) {
            limits.loginFailuresPerEmail.tryAcquire(emailKey);
            throw badCredentials();
        }
        UserAccount user = found.get();
        if (user.getStatus() == UserStatus.LOCKED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This account is locked");
        }

        return new LoginResponse(jwt.issue(user), UserDto.of(user));
    }

    /**
     * Sign-up creates a pending request; an admin turns it into an account. The
     * answer is the same whether or not the email already has one.
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void register(@Valid @RequestBody RegisterRequest request, HttpServletRequest http) {
        if (!limits.registerPerAddress.tryAcquire(http.getRemoteAddr())) {
            throw new TooManyRequestsException();
        }
        userService.register(request);
    }

    @GetMapping("/me")
    public UserDto me() {
        return UserDto.of(CurrentUser.require());
    }

    private static ResponseStatusException badCredentials() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Wrong email or password");
    }
}
