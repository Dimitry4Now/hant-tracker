package com.hanttracker.api.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "game")
@Getter
@Setter
@NoArgsConstructor
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate playedOn;

    /** Nth game of that day — more than one game can share a date. */
    @Column(nullable = false)
    private int gameOfDay;

    @Column(nullable = false)
    private boolean inProgress = true;

    /** Set once a Majstorska (final) round is recorded. */
    @Column(nullable = false)
    private boolean majstorska;

    @Column(length = 500)
    private String note;

    /** Seating order, which is also the order round entries are stored in. */
    @ElementCollection
    @CollectionTable(name = "game_player", joinColumns = @JoinColumn(name = "game_id"))
    @OrderColumn(name = "seat")
    @Column(name = "player_id", nullable = false)
    private List<Long> playerIds = new ArrayList<>();

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("number ASC")
    private List<Round> rounds = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "game_money", joinColumns = @JoinColumn(name = "game_id"))
    private List<MoneyEntry> money = new ArrayList<>();

    public void addRound(Round round) {
        round.setGame(this);
        rounds.add(round);
    }
}
