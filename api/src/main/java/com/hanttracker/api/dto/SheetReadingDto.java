package com.hanttracker.api.dto;

import java.util.List;

/**
 * What was read off a photo of a paper score sheet. Rows come top to bottom as
 * written; the client turns the running totals into rounds and checks them
 * against the rules, so a misread cell shows up there rather than here.
 *
 * @param headers the column labels, usually the players' initials
 * @param date the date written on the sheet as yyyy-MM-dd, or null
 */
public record SheetReadingDto(List<String> headers, String date, List<Row> rows) {

    /**
     * One row of the sheet. {@code totals} and {@code uncertain} have one cell per
     * column; a cell that could not be read is null.
     */
    public record Row(RowKind kind, List<Integer> totals, List<Boolean> uncertain) {}

    /** A round's running totals, or part of the money worked out under them. */
    public enum RowKind {
        ROUND,
        MONEY
    }
}
