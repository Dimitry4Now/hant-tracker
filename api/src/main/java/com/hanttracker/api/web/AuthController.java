package com.hanttracker.api.web;

import com.hanttracker.api.domain.UserAccount;
import com.hanttracker.api.domain.UserStatus;
import com.hanttracker.api.dto.AccountRequestDto;
import com.hanttracker.api.dto.AuthDtos.LoginRequest;
import com.hanttracker.api.dto.AuthDtos.LoginResponse;
import com.hanttracker.api.dto.AuthDtos.RegisterRequest;
import com.hanttracker.api.dto.UserDto;
import com.hanttracker.api.repo.UserAccountRepository;
import com.hanttracker.api.security.CurrentUser;
import com.hanttracker.api.security.JwtService;
import com.hanttracker.api.service.UserService;
import jakarta.validation.Valid;
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

    public AuthController(
            UserAccountRepository users,
            PasswordEncoder encoder,
            JwtService jwt,
            UserService userService) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
        this.userService = userService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        UserAccount user =
                users.findByEmailIgnoreCase(request.email().trim()).orElseThrow(AuthController::badCredentials);

        if (!encoder.matches(request.password(), user.getPasswordHash())) {
            throw badCredentials();
        }
        if (user.getStatus() == UserStatus.LOCKED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This account is locked");
        }

        return new LoginResponse(jwt.issue(user), UserDto.of(user));
    }

    /** Sign-up creates a pending request; an admin turns it into an account. */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public AccountRequestDto register(@Valid @RequestBody RegisterRequest request) {
        return userService.register(request);
    }

    @GetMapping("/me")
    public UserDto me() {
        return UserDto.of(CurrentUser.require());
    }

    private static ResponseStatusException badCredentials() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Wrong email or password");
    }
}
