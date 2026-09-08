package com.hanttracker.api.dto;

public record LeaderboardRowDto(Long playerId, String name, int points, int wins, int losses, int netDen) {}
