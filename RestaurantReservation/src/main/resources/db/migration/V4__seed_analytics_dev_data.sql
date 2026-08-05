-- Extra reservation seed data so the Analytics page shows meaningful shape.
-- V2 only inserts today's rows, which collapses every daily / weekly /
-- monthly aggregation to a single bucket. This migration wipes the
-- reservation table and repopulates it with:
--   (a) today's status-diverse demo set that the Reservations / Dashboard
--       pages rely on (mirrors V2's intent);
--   (b) 9 weeks of historical reservations shaped like a real restaurant —
--       lunch and dinner slots, weekends noticeably busier than weekdays,
--       19:00 as the dinner peak, mostly COMPLETED with a handful of
--       CANCELLED / NO_SHOW so the status KPIs stay meaningful.
DELETE FROM reservations;

-- (a) Today's variety pack — one reservation per active status.
INSERT INTO reservations (restaurant_id, restaurant_table_id, guest_id, party_size,
                          start_time, end_time, status, notes, reminder_sent)
VALUES
    (1, 1, 1, 2, NOW()::date + INTERVAL '12 hours',
                 NOW()::date + INTERVAL '13 hours 30 minutes',
     'CONFIRMED', 'Window table please', false),
    (1, 2, 2, 3, NOW()::date + INTERVAL '13 hours',
                 NOW()::date + INTERVAL '14 hours 30 minutes',
     'CONFIRMED', NULL, false),
    (1, 3, 3, 4, NOW()::date + INTERVAL '11 hours',
                 NOW()::date + INTERVAL '12 hours 30 minutes',
     'SEATED', NULL, true),
    (1, 4, 4, 5, NOW()::date + INTERVAL '14 hours',
                 NOW()::date + INTERVAL '15 hours 30 minutes',
     'PENDING', 'Birthday dinner', false),
    (1, 5, 1, 2, NOW()::date + INTERVAL '9 hours',
                 NOW()::date + INTERVAL '10 hours 30 minutes',
     'COMPLETED', NULL, true),
    (1, 1, 2, 2, NOW()::date + INTERVAL '10 hours',
                 NOW()::date + INTERVAL '11 hours 30 minutes',
     'CANCELLED', NULL, false),
    (1, 6, 3, 1, NOW()::date + INTERVAL '10 hours 30 minutes',
                 NOW()::date + INTERVAL '12 hours',
     'NO_SHOW', NULL, true);

-- (b) Historical reservations over the last 63 days (9 full weeks).
-- Slot design (dow uses PostgresSQL convention: 0 = Sunday, 6 = Saturday):
--   Weekdays Tue–Thu: light lunch + steady dinner (2–3 reservations/day).
--   Monday:           dinner only (quiet).
--   Fri & Sat:        full lunch + dinner rush (8 reservations/day each).
--   Sun:              busy lunch + moderate dinner (5 reservations/day).
-- Tables are paired to slots so the same table never overlaps itself on a
-- given day (90-minute duration). Party sizes stay within table capacity.
INSERT INTO reservations (restaurant_id, restaurant_table_id, guest_id, party_size,
                          start_time, end_time, status, notes, reminder_sent)
WITH days AS (
    SELECT
        d::date AS day,
        EXTRACT(DOW FROM d)::int AS dow
    FROM generate_series(
        CURRENT_DATE - INTERVAL '63 days',
        CURRENT_DATE - INTERVAL '1 day',
        INTERVAL '1 day'
    ) d
),
slots AS (
    -- Weekend lunches (Sun / Fri / Sat)
    SELECT day, TIME '12:00' AS slot_time, 1 AS table_id FROM days WHERE dow IN (0, 5, 6)
    UNION ALL
    SELECT day, TIME '13:00', 2 FROM days WHERE dow IN (0, 5, 6)
    UNION ALL
    SELECT day, TIME '13:30', 3 FROM days WHERE dow IN (5, 6)
    -- Weekday lunches (Tue / Wed / Thu)
    UNION ALL
    SELECT day, TIME '13:00', 2 FROM days WHERE dow IN (2, 3, 4)
    -- Dinner — 19:00 every day (steady baseline / peak hour)
    UNION ALL
    SELECT day, TIME '19:00', 4 FROM days
    -- Later dinner (Sun / Thu / Fri / Sat)
    UNION ALL
    SELECT day, TIME '19:30', 5 FROM days WHERE dow IN (0, 4, 5, 6)
    -- Fri / Sat late dinner rush
    UNION ALL
    SELECT day, TIME '20:00', 6 FROM days WHERE dow IN (5, 6)
    UNION ALL
    SELECT day, TIME '20:30', 7 FROM days WHERE dow IN (0, 5, 6)
    UNION ALL
    SELECT day, TIME '21:00', 3 FROM days WHERE dow IN (5, 6)
),
numbered AS (
    SELECT
        s.day,
        s.slot_time,
        s.table_id,
        row_number() OVER (ORDER BY s.day, s.slot_time, s.table_id) AS rn
    FROM slots s
)
SELECT
    1 AS restaurant_id,
    n.table_id AS restaurant_table_id,
    ((n.rn - 1) % 4) + 1 AS guest_id,
    CASE n.table_id
        WHEN 1 THEN 2
        WHEN 2 THEN 3
        WHEN 3 THEN 4
        WHEN 4 THEN 5
        WHEN 5 THEN 7
        WHEN 6 THEN 2
        WHEN 7 THEN 4
    END AS party_size,
    n.day + n.slot_time AS start_time,
    n.day + n.slot_time + INTERVAL '90 minutes' AS end_time,
    CASE
        WHEN n.rn % 14 = 0 THEN 'CANCELLED'
        WHEN n.rn % 13 = 0 THEN 'NO_SHOW'
        ELSE 'COMPLETED'
    END AS status,
    NULL AS notes,
    true AS reminder_sent
FROM numbered n;
