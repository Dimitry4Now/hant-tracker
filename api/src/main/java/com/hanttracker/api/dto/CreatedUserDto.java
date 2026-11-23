package com.hanttracker.api.dto;

/**
 * A player an admin added. {@code temporaryPassword} is the generated one when
 * the admin left the password out, shown once; null when they chose one.
 */
public record CreatedUserDto(UserDto user, String temporaryPassword) {}
