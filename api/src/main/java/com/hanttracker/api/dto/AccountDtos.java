package com.hanttracker.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request bodies for /api/account — the logged-in user editing themselves. */
public final class AccountDtos {

    private AccountDtos() {}

    /** The current password is only checked when the email changes. */
    public record UpdateProfileRequest(
            @NotBlank String displayName, @NotBlank @Email String email, String currentPassword) {}

    public record ChangePasswordRequest(
            @NotBlank String currentPassword, @NotBlank @Size(min = 8) String newPassword) {}
}
