-- Restaurant
INSERT INTO restaurants (name, address, phone, default_reservation_minutes, grace_period_minutes)
VALUES ('Demo Restaurant', 'Tbilisi, Rustaveli 1', '+995599000000', 90, 15);
-- Users — BCrypt of "password123"
INSERT INTO users (full_name, email, password_hash, role, restaurant_id)
VALUES ('Anna Owner', 'owner@demo.com', '$2a$10$O0fPOsEbD0qeCBKYu/GU6uzEyGhB1gVOZQGd5R/mJ/EcgKjf1J6By', 'OWNER', 1),
       ('Maria Manager', 'manager@demo.com', '$2a$10$O0fPOsEbD0qeCBKYu/GU6uzEyGhB1gVOZQGd5R/mJ/EcgKjf1J6By', 'MANAGER', 1),
       ('Sandro Staff', 'staff@demo.com', '$2a$10$O0fPOsEbD0qeCBKYu/GU6uzEyGhB1gVOZQGd5R/mJ/EcgKjf1J6By', 'STAFF', 1);
-- Tables
INSERT INTO restaurant_tables (label, capacity, status, restaurant_id)
VALUES ('T1', 2, 'AVAILABLE', 1),
       ('T2', 4, 'AVAILABLE', 1),
       ('T3', 4, 'AVAILABLE', 1),
       ('T4', 6, 'AVAILABLE', 1),
       ('T5', 8, 'AVAILABLE', 1),
       ('Bar 1', 2, 'AVAILABLE', 1),
       ('Terrace 1', 6, 'AVAILABLE', 1);
-- Guests
INSERT INTO guests (full_name, phone, restaurant_id)
VALUES ('Giorgi Beridze', '+995599111001', 1),
       ('Nino Kvaratskhelia', '+995599111002', 1),
       ('David Kiknadze', '+995599111003', 1),
       ('Tamar Lomidze', '+995599111004', 1);
-- Reservations today covering all statuses
INSERT INTO reservations (restaurant_id, restaurant_table_id, guest_id, party_size, start_time, end_time, status, notes,
                          reminder_sent)
VALUES (1, 1, 1, 2, NOW()::date + INTERVAL '12 hours', NOW()::date + INTERVAL '13 hours 30 minutes', 'CONFIRMED',
        'Window table please', false),
       (1, 2, 2, 3, NOW()::date + INTERVAL '13 hours', NOW()::date + INTERVAL '14 hours 30 minutes', 'CONFIRMED', NULL,
        false),
       (1, 3, 3, 4, NOW()::date + INTERVAL '11 hours', NOW()::date + INTERVAL '12 hours 30 minutes', 'SEATED', NULL,
        true),
       (1, 4, 4, 5, NOW()::date + INTERVAL '14 hours', NOW()::date + INTERVAL '15 hours 30 minutes', 'PENDING',
        'Birthday dinner', false),
       (1, 5, 1, 2, NOW()::date + INTERVAL '9 hours', NOW()::date + INTERVAL '10 hours 30 minutes', 'COMPLETED', NULL,
        true),
       (1, 1, 2, 2, NOW()::date + INTERVAL '10 hours', NOW()::date + INTERVAL '11 hours 30 minutes', 'CANCELLED', NULL,
        false),
       (1, 6, 3, 1, NOW()::date + INTERVAL '10 hours 30 minutes', NOW()::date + INTERVAL '12 hours', 'NO_SHOW', NULL,
        true);
