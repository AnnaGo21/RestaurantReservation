CREATE TABLE restaurants (
     id BIGSERIAL PRIMARY KEY,
     name VARCHAR(255) NOT NULL,
     phone VARCHAR(50),
     address VARCHAR(500),
     timezone VARCHAR(100) DEFAULT 'UTC',
     default_reservation_minutes INTEGER DEFAULT 90,
     grace_period_minutes INTEGER DEFAULT 15,
     cuisine_type VARCHAR(255),
     opening_hours TEXT,
     logo_url VARCHAR(500),
     CONSTRAINT chk_restaurants_default_reservation_minutes CHECK (default_reservation_minutes > 0),
     CONSTRAINT chk_restaurants_grace_period_minutes CHECK (grace_period_minutes >= 0)
);

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    restaurant_id BIGINT NOT NULL,
    CONSTRAINT fk_users_restaurant
        FOREIGN KEY (restaurant_id)
            REFERENCES restaurants(id)
            ON DELETE CASCADE,

    CONSTRAINT chk_users_role
        CHECK (role IN ('OWNER', 'MANAGER', 'STAFF'))
);

CREATE TABLE restaurant_tables (
    id BIGSERIAL PRIMARY KEY,
    label VARCHAR(100),
    capacity INTEGER NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'AVAILABLE',
    position_x DOUBLE PRECISION,
    position_y DOUBLE PRECISION,
    restaurant_id BIGINT NOT NULL,
    CONSTRAINT fk_restaurant_tables_restaurant
        FOREIGN KEY (restaurant_id)
        REFERENCES restaurants(id)
        ON DELETE CASCADE,
    CONSTRAINT chk_restaurant_tables_capacity CHECK (capacity > 0),
    CONSTRAINT chk_restaurant_tables_status CHECK (status IN ('AVAILABLE', 'RESERVED', 'OUT_OF_SERVICE'))
);

CREATE TABLE guests (
    id BIGSERIAL PRIMARY KEY,
    restaurant_id BIGINT NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    phone VARCHAR(50) NOT NULL,
    email VARCHAR(255),
    total_visits INTEGER DEFAULT 0,
    no_show_count INTEGER DEFAULT 0,
    CONSTRAINT fk_guests_restaurant
        FOREIGN KEY (restaurant_id)
        REFERENCES restaurants(id)
        ON DELETE CASCADE,
    CONSTRAINT uq_guests_restaurant_phone UNIQUE (restaurant_id, phone),
    CONSTRAINT chk_guests_total_visits CHECK (total_visits >= 0),
    CONSTRAINT chk_guests_no_show_count CHECK (no_show_count >= 0)
);

CREATE TABLE reservations (
    id BIGSERIAL PRIMARY KEY,
    restaurant_id BIGINT NOT NULL,
    restaurant_table_id BIGINT NOT NULL,
    guest_id BIGINT NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    party_size INTEGER NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'CONFIRMED',
    reminder_sent BOOLEAN DEFAULT FALSE,
    checked_in_at TIMESTAMP,
    notes TEXT,
    CONSTRAINT fk_reservations_restaurant
        FOREIGN KEY (restaurant_id)
        REFERENCES restaurants(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_reservations_restaurant_table
        FOREIGN KEY (restaurant_table_id)
        REFERENCES restaurant_tables(id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_reservations_guest
        FOREIGN KEY (guest_id)
        REFERENCES guests(id)
        ON DELETE RESTRICT,
    CONSTRAINT chk_reservations_party_size CHECK (party_size > 0),
    CONSTRAINT chk_reservations_time_range CHECK (end_time > start_time),
    CONSTRAINT chk_reservations_status CHECK (
        status IN (
            'PENDING',
            'CONFIRMED',
            'SEATED',
            'COMPLETED',
            'CANCELLED',
            'NO_SHOW'
        )
    )
);

CREATE INDEX idx_users_email
    ON users(email);

CREATE INDEX idx_users_restaurant_id
    ON users(restaurant_id);

CREATE INDEX idx_restaurant_tables_restaurant_id
    ON restaurant_tables(restaurant_id);

CREATE INDEX idx_guests_restaurant_id
    ON guests(restaurant_id);

CREATE INDEX idx_reservations_restaurant_start_time
    ON reservations(restaurant_id, start_time);

CREATE INDEX idx_reservations_table_time_range
    ON reservations(restaurant_table_id, start_time, end_time);

CREATE INDEX idx_reservations_status
    ON reservations(status);

CREATE INDEX idx_reservations_guest_id
    ON reservations(guest_id);