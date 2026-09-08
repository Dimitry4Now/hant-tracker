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
