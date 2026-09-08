package com.hanttracker.api.dto;

import java.util.List;

/** Everything the signed-out landing page shows. Percentages are whole numbers. */
public record PublicStatsDto(
        int startYear,
        String gamesRange,
        List<MonthGamesDto> gamesByMonth,
        int gamesPlayed,
        int roundsRecorded,
        int hantsRecorded,
        int players,
        int roundsEndingInHant,
        int roundsClosedRegular,
        int gamesWithQuitter,
        List<RankingDto> rankings,
        List<PlaystyleDto> playstyles) {

    public record RankingDto(String name, int games, int winRate) {}

    public record PlaystyleDto(String name, String style) {}
}
