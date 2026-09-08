package com.hanttracker.api.dto;

import com.hanttracker.api.domain.Round;
import java.util.List;

public record RoundDto(
        Long id,
        int number,
        Long dealerId,
        boolean majstorska,
        boolean hant,
        String comment,
        List<RoundEntryDto> entries) {

    public static RoundDto of(Round round) {
        return new RoundDto(
                round.getId(),
                round.getNumber(),
                round.getDealerId(),
                round.isMajstorska(),
                round.isHant(),
                round.getComment(),
                round.getEntries().stream().map(RoundEntryDto::of).toList());
    }
}
