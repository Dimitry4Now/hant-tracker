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
        LocalDate joinedOn) {

    /**
     * What other players see: enough to show names in games and standings,
     * without anyone's full name or email.
     */
    public static UserDto forPlayers(UserAccount user) {
        return new UserDto(
                user.getId(), null, user.getDisplayName(), null, user.getRole(), user.getStatus(), user.getJoinedOn());
    }

    public static UserDto of(UserAccount user) {
        return new UserDto(
                user.getId(),
                user.getFullName(),
                user.getDisplayName(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getJoinedOn());
    }
}
