package com.hanttracker.api.dto;

import com.hanttracker.api.domain.AccountRequest;
import java.time.LocalDate;

public record AccountRequestDto(Long id, String fullName, String email, LocalDate requestedOn) {

    public static AccountRequestDto of(AccountRequest request) {
        return new AccountRequestDto(
                request.getId(), request.getFullName(), request.getEmail(), request.getRequestedOn());
    }
}
