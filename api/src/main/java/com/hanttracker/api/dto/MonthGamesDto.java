package com.hanttracker.api.dto;

import java.util.List;

/** Games played in each week of one month — a week holds 0–2 games. */
public record MonthGamesDto(String label, List<Integer> weeks) {}
