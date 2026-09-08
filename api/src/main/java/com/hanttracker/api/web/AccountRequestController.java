package com.hanttracker.api.web;

import com.hanttracker.api.dto.AccountRequestDto;
import com.hanttracker.api.dto.UserDto;
import com.hanttracker.api.service.UserService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Admin-only queue of pending sign-ups. */
@RestController
@RequestMapping("/api/account-requests")
public class AccountRequestController {

    private final UserService users;

    public AccountRequestController(UserService users) {
        this.users = users;
    }

    @GetMapping
    public List<AccountRequestDto> list() {
        return users.listRequests();
    }

    @PostMapping("/{id}/approve")
    public UserDto approve(@PathVariable Long id) {
        return users.approveRequest(id);
    }

    @PostMapping("/{id}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reject(@PathVariable Long id) {
        users.rejectRequest(id);
    }
}
