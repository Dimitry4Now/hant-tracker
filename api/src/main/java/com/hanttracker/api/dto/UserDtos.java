package com.hanttracker.api.dto;

import com.hanttracker.api.domain.UserRole;
import com.hanttracker.api.domain.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Request bodies for /api/users. */
public final class UserDtos {

    private UserDtos() {}

    /**
     * Password is optional — when an admin adds a player by hand and leaves it
     * out, the account gets {@code app.default-password} and should be changed.
     */
    public record CreateUserRequest(
            @NotBlank String fullName,
            @NotBlank String displayName,
            @NotBlank @Email String email,
            @NotNull UserRole role,
            String password) {}

    public record UpdateStatusRequest(@NotNull UserStatus status) {}

    public record RenameRequest(@NotBlank String displayName) {}

    public record ChangeEmailRequest(@NotBlank @Email String email) {}
}
