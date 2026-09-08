package com.hanttracker.api.web;

import com.hanttracker.api.dto.UserDto;
import com.hanttracker.api.dto.UserDtos.CreateUserRequest;
import com.hanttracker.api.dto.UserDtos.RenameRequest;
import com.hanttracker.api.dto.UserDtos.UpdateStatusRequest;
import com.hanttracker.api.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService users;

    public UserController(UserService users) {
        this.users = users;
    }

    @GetMapping
    public List<UserDto> list() {
        return users.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto create(@Valid @RequestBody CreateUserRequest request) {
        return users.create(request);
    }

    @PatchMapping("/{id}/status")
    public UserDto setStatus(@PathVariable Long id, @Valid @RequestBody UpdateStatusRequest request) {
        return users.setStatus(id, request.status());
    }

    @PatchMapping("/{id}/name")
    public UserDto rename(@PathVariable Long id, @Valid @RequestBody RenameRequest request) {
        return users.rename(id, request.displayName());
    }
}
