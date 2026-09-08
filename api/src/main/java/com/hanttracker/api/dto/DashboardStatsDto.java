package com.hanttracker.api.dto;

import java.util.List;

/** The signed-in player's own numbers, plus the all-time leaderboard. */
public record DashboardStatsDto(
        int gamesThisMonth,
        int gamesLastMonth,
        int netDenThisMonth,
        int netDenLastMonth,
        int winRateThisMonth,
        int winRateLastMonth,
        int allTimeWinRate,
        int allTimeGames,
        int allTimeNetDen,
        List<LeaderboardRowDto> leaderboard) {}
