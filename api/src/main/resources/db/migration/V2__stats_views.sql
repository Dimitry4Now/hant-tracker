-- Foreign keys the stats and game lookups filter on, which had no index.
CREATE INDEX idx_round_game ON round (game_id);
CREATE INDEX idx_game_money_game ON game_money (game_id);
CREATE INDEX idx_game_played_on ON game (played_on);

-- One row per player per game, with everything the stats pages add up.
--
-- place ranks the players within the game: lowest total points wins, and a
-- tie goes to whoever sits first. A player with no rounds yet counts as 0
-- points. money_den is NULL until the money for that game is entered.
-- Counts are cast to INTEGER so PostgreSQL and H2 report the same type.
CREATE VIEW player_game_stats AS
SELECT
    s.game_id,
    s.player_id,
    s.seat,
    s.played_on,
    s.in_progress,
    s.points,
    CAST(ROW_NUMBER() OVER (PARTITION BY s.game_id ORDER BY s.points, s.seat) AS INTEGER) AS place,
    s.rounds_played,
    s.rounds_won,
    s.rounds_opened,
    s.rounds_not_opened,
    s.rounds_quit,
    s.money_den
FROM (
    SELECT
        gp.game_id,
        gp.player_id,
        gp.seat,
        g.played_on,
        g.in_progress,
        CAST(COALESCE(e.points, 0) AS INTEGER)            AS points,
        CAST(COALESCE(e.rounds_played, 0) AS INTEGER)     AS rounds_played,
        CAST(COALESCE(e.rounds_won, 0) AS INTEGER)        AS rounds_won,
        CAST(COALESCE(e.rounds_opened, 0) AS INTEGER)     AS rounds_opened,
        CAST(COALESCE(e.rounds_not_opened, 0) AS INTEGER) AS rounds_not_opened,
        CAST(COALESCE(e.rounds_quit, 0) AS INTEGER)       AS rounds_quit,
        CAST(m.amount_den AS INTEGER)                     AS money_den
    FROM game_player gp
    JOIN game g ON g.id = gp.game_id
    LEFT JOIN (
        SELECT
            r.game_id,
            re.player_id,
            SUM(re.points)                                             AS points,
            COUNT(*)                                                   AS rounds_played,
            SUM(CASE WHEN re.outcome = 'WINNER' THEN 1 ELSE 0 END)     AS rounds_won,
            SUM(CASE WHEN re.outcome = 'OPENED' THEN 1 ELSE 0 END)     AS rounds_opened,
            SUM(CASE WHEN re.outcome = 'NOT_OPENED' THEN 1 ELSE 0 END) AS rounds_not_opened,
            SUM(CASE WHEN re.outcome = 'QUIT' THEN 1 ELSE 0 END)       AS rounds_quit
        FROM round r
        JOIN round_entry re ON re.round_id = r.id
        GROUP BY r.game_id, re.player_id
    ) e ON e.game_id = gp.game_id AND e.player_id = gp.player_id
    LEFT JOIN (
        SELECT game_id, player_id, SUM(amount_den) AS amount_den
        FROM game_money
        GROUP BY game_id, player_id
    ) m ON m.game_id = gp.game_id AND m.player_id = gp.player_id
) s;

-- One row per game with its round counts, for the public landing page.
-- A regular round is one that is not a Hant and that someone won outright.
CREATE VIEW game_summary AS
SELECT
    g.id AS game_id,
    g.played_on,
    g.in_progress,
    CAST(COALESCE(r.rounds, 0) AS INTEGER)         AS rounds,
    CAST(COALESCE(r.hant_rounds, 0) AS INTEGER)    AS hant_rounds,
    CAST(COALESCE(r.regular_rounds, 0) AS INTEGER) AS regular_rounds
FROM game g
LEFT JOIN (
    SELECT
        rr.game_id,
        COUNT(*)                                                    AS rounds,
        SUM(CASE WHEN rr.hant THEN 1 ELSE 0 END)                    AS hant_rounds,
        SUM(CASE WHEN NOT rr.hant AND rr.has_winner THEN 1 ELSE 0 END) AS regular_rounds
    FROM (
        SELECT
            r.game_id,
            r.hant,
            EXISTS (
                SELECT 1 FROM round_entry re
                WHERE re.round_id = r.id AND re.outcome = 'WINNER'
            ) AS has_winner
        FROM round r
    ) rr
    GROUP BY rr.game_id
) r ON r.game_id = g.id;
