-- Table & Reservation module (owner: Kieu Bao Giang)
-- Matches docs/diagrams/giang/04-erd-1-table-reservation.png

CREATE TABLE users (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    CONSTRAINT uk_users_username UNIQUE (username)
);

CREATE TABLE table_section (
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    CONSTRAINT uk_table_section_name UNIQUE (name)
);

CREATE TABLE dining_table (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    number     INT         NOT NULL,
    capacity   INT         NOT NULL,
    section_id BIGINT      NOT NULL,
    status     VARCHAR(20) NOT NULL,
    active     BOOLEAN     NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_dining_table_number UNIQUE (number),
    CONSTRAINT ck_dining_table_capacity CHECK (capacity BETWEEN 1 AND 20),
    CONSTRAINT fk_dining_table_section FOREIGN KEY (section_id) REFERENCES table_section (id)
);

CREATE TABLE reservation (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    code       VARCHAR(12)  NOT NULL,
    table_id   BIGINT       NOT NULL,
    guest_name VARCHAR(100) NOT NULL,
    phone      VARCHAR(15)  NOT NULL,
    party_size INT          NOT NULL,
    start_time DATETIME     NOT NULL,
    note       VARCHAR(200),
    status     VARCHAR(20)  NOT NULL,
    CONSTRAINT uk_reservation_code UNIQUE (code),
    CONSTRAINT fk_reservation_table FOREIGN KEY (table_id) REFERENCES dining_table (id)
);
CREATE INDEX ix_reservation_table_start ON reservation (table_id, start_time);

CREATE TABLE waitlist_entry (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    party_name      VARCHAR(100) NOT NULL,
    phone           VARCHAR(15)  NOT NULL,
    party_size      INT          NOT NULL,
    arrived_at      DATETIME     NOT NULL,
    seated_table_id BIGINT,
    CONSTRAINT fk_waitlist_seated_table FOREIGN KEY (seated_table_id) REFERENCES dining_table (id)
);

CREATE TABLE reservation_settings (
    id             BIGINT PRIMARY KEY,
    dining_minutes INT NOT NULL DEFAULT 180,
    turn_minutes   INT NOT NULL DEFAULT 15
);
INSERT INTO reservation_settings (id, dining_minutes, turn_minutes) VALUES (1, 180, 15);
