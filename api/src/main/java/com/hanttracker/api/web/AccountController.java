package com.hanttracker.api.web;

import com.hanttracker.api.dto.AccountDtos.ChangePasswordRequest;
import com.hanttracker.api.dto.AccountDtos.UpdateProfileRequest;
import com.hanttracker.api.dto.UserDto;
import com.hanttracker.api.security.CurrentUser;
import com.hanttracker.api.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Lets any logged-in user edit their own account. */
@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final UserService users;

    public AccountController(UserService users) {
        this.users = users;
    }

    @PutMapping("/profile")
    public UserDto updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return users.updateProfile(CurrentUser.require().getId(), request);
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        users.changePassword(CurrentUser.require().getId(), request);
    }
}
