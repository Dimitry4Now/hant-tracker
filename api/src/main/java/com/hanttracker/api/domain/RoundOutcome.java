package com.hanttracker.api.domain;

/** How a player finished a single round. Drives the points column. */
public enum RoundOutcome {
    WINNER,
    OPENED,
    NOT_OPENED,
    QUIT
}
