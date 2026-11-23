package com.hanttracker.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A code emailed to confirm an address; only its SHA-256 is kept. */
@Entity
@Table(name = "email_code")
@Getter
@Setter
@NoArgsConstructor
public class EmailCode {

    @Id
    private Long userId;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false, length = 64)
    private String codeHash;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private int attempts;
}
