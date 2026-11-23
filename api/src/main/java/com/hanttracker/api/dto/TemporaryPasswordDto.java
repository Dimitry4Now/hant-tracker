package com.hanttracker.api.dto;

/** A one-time password an admin set; shown once, for them to pass on. */
public record TemporaryPasswordDto(String temporaryPassword) {}
