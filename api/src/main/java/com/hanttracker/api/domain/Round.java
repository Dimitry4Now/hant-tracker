package com.hanttracker.api.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "round")
@Getter
@Setter
@NoArgsConstructor
public class Round {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    /** 1-based position within the game. */
    @Column(name = "number", nullable = false)
    private int number;

    @Column(nullable = false)
    private Long dealerId;

    @Column(nullable = false)
    private boolean majstorska;

    @Column(nullable = false)
    private boolean hant;

    @Column(length = 500)
    private String comment;

    @ElementCollection
    @CollectionTable(name = "round_entry", joinColumns = @JoinColumn(name = "round_id"))
    @OrderColumn(name = "seat")
    private List<RoundEntryValue> entries = new ArrayList<>();
}
