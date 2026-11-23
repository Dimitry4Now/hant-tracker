package com.hanttracker.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request and response bodies for /api/auth. */
public final class AuthDtos {

    private AuthDtos() {}

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

    public record LoginResponse(String token, UserDto user) {}

    /** What the login page can offer. */
    public record AuthOptions(boolean passwordResetByEmail) {}

    public record ForgotPasswordRequest(@NotBlank @Email String email) {}

    public record ResetPasswordRequest(@NotBlank String token, @NotBlank @Size(min = 8) String newPassword) {}

    public record RegisterRequest(
            @NotBlank String fullName,
            @NotBlank String displayName,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8) String password) {}
}
