package com.hanttracker.api.dto;

import com.hanttracker.api.domain.UserAccount;
import com.hanttracker.api.domain.UserRole;
import com.hanttracker.api.domain.UserStatus;
import java.time.LocalDate;

public record UserDto(
        Long id,
        String fullName,
        String displayName,
        String email,
        UserRole role,
        UserStatus status,
        LocalDate joinedOn,
        /** For admins: this player still has to replace their password. */
        boolean mustChangePassword,
        /** The player confirmed this email with a code. */
        boolean emailVerified,
        /** Only in the player's own session: what they must do first, if anything. */
        RequiredStep requiredStep) {

    /**
     * What other players see: enough to show names in games and standings,
     * without anyone's full name or email.
     */
    public static UserDto forPlayers(UserAccount user) {
        return new UserDto(
                user.getId(),
                null,
                user.getDisplayName(),
                null,
                user.getRole(),
                user.getStatus(),
                user.getJoinedOn(),
                false,
                false,
                null);
    }

    /** The signed-in player's own account, with the step they must take first. */
    public static UserDto forSession(UserAccount user, RequiredStep requiredStep) {
        UserDto dto = of(user);
        return new UserDto(
                dto.id(),
                dto.fullName(),
                dto.displayName(),
                dto.email(),
                dto.role(),
                dto.status(),
                dto.joinedOn(),
                dto.mustChangePassword(),
                dto.emailVerified(),
                requiredStep);
    }

    public static UserDto of(UserAccount user) {
        return new UserDto(
                user.getId(),
                user.getFullName(),
                user.getDisplayName(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getJoinedOn(),
                user.isMustChangePassword(),
                user.isEmailVerified(),
                null);
    }
}
