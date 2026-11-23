package com.hanttracker.api.web;

import com.hanttracker.api.dto.AccountDtos.ChangePasswordRequest;
import com.hanttracker.api.dto.AccountDtos.ConfirmEmailRequest;
import com.hanttracker.api.dto.AccountDtos.EmailRequest;
import com.hanttracker.api.dto.AccountDtos.UpdateProfileRequest;
import com.hanttracker.api.dto.AuthDtos.LoginResponse;
import com.hanttracker.api.dto.UserDto;
import com.hanttracker.api.domain.UserAccount;
import com.hanttracker.api.security.AccountSetup;
import com.hanttracker.api.security.CurrentUser;
import com.hanttracker.api.security.JwtService;
import com.hanttracker.api.service.EmailAccountService;
import com.hanttracker.api.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Lets any logged-in user edit their own account. */
@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final UserService users;
    private final EmailAccountService email;
    private final JwtService jwt;
    private final AccountSetup setup;

    public AccountController(UserService users, EmailAccountService email, JwtService jwt, AccountSetup setup) {
        this.users = users;
        this.email = email;
        this.jwt = jwt;
        this.setup = setup;
    }

    /** Emails a code to the address; it becomes the account's once confirmed. */
    @PostMapping("/email")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void sendEmailCode(@Valid @RequestBody EmailRequest request) {
        email.sendCode(CurrentUser.require().getId(), request.email());
    }

    @PostMapping("/email/confirm")
    public UserDto confirmEmail(@Valid @RequestBody ConfirmEmailRequest request) {
        UserAccount user = email.confirmCode(CurrentUser.require().getId(), request.code());
        return UserDto.forSession(user, setup.requiredStep(user));
    }

    @PutMapping("/profile")
    public UserDto updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return users.updateProfile(CurrentUser.require().getId(), request);
    }

    /**
     * Other sessions are signed out by the change, this one included; the answer
     * carries a new token for it.
     */
    @PutMapping("/password")
    public LoginResponse changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        UserAccount user = users.changePassword(CurrentUser.require().getId(), request);
        return new LoginResponse(jwt.issue(user), UserDto.forSession(user, setup.requiredStep(user)));
    }
}
