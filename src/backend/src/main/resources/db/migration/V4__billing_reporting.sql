-- Billing & Reporting module (owner: Nguyen Nam Khoa)
-- Matches docs/diagrams/khoa/updated/w9-erd-billing.png
-- Money is stored as whole VND in BIGINT columns, never as floating-point numbers (NFR-08).

-- open_marker is 1 while the shift is open and NULL once it is closed. The unique key allows many
-- NULLs but only one 1, so the database itself refuses a second open shift (BR-13).
CREATE TABLE shift (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    opened_at    DATETIME NOT NULL,
    closed_at    DATETIME,
    opened_by_id BIGINT   NOT NULL,
    closed_by_id BIGINT,
    counted_cash BIGINT,
    open_marker  TINYINT,
    CONSTRAINT uk_shift_open_marker UNIQUE (open_marker),
    CONSTRAINT ck_shift_counted_cash CHECK (counted_cash IS NULL OR counted_cash >= 0),
    CONSTRAINT fk_shift_opened_by FOREIGN KEY (opened_by_id) REFERENCES users (id),
    CONSTRAINT fk_shift_closed_by FOREIGN KEY (closed_by_id) REFERENCES users (id)
);
CREATE INDEX ix_shift_opened_at ON shift (opened_at);

-- An order has at most one bill that is not cancelled (BR-09); the service checks it with the order row locked.
-- shift_id is the shift in which the bill was closed, so the shift report can count it (FR-28).
CREATE TABLE bill (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id   BIGINT      NOT NULL,
    total      BIGINT      NOT NULL,
    status     VARCHAR(20) NOT NULL,
    split_type VARCHAR(20) NOT NULL,
    created_at DATETIME    NOT NULL,
    closed_at  DATETIME,
    shift_id   BIGINT,
    CONSTRAINT ck_bill_total CHECK (total > 0),
    CONSTRAINT fk_bill_order FOREIGN KEY (order_id) REFERENCES customer_order (id),
    CONSTRAINT fk_bill_shift FOREIGN KEY (shift_id) REFERENCES shift (id)
);
CREATE INDEX ix_bill_order ON bill (order_id);
CREATE INDEX ix_bill_status ON bill (status);
CREATE INDEX ix_bill_shift ON bill (shift_id);

CREATE TABLE bill_share (
    id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    bill_id BIGINT NOT NULL,
    number  INT    NOT NULL,
    amount  BIGINT NOT NULL,
    CONSTRAINT uk_bill_share_number UNIQUE (bill_id, number),
    CONSTRAINT ck_bill_share_amount CHECK (amount > 0),
    CONSTRAINT fk_bill_share_bill FOREIGN KEY (bill_id) REFERENCES bill (id)
);

-- Copied from the order lines when the bill is generated, so a later menu change cannot alter a bill.
-- share_id is set only when the bill is split by item (FR-24).
CREATE TABLE bill_line (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    bill_id    BIGINT       NOT NULL,
    share_id   BIGINT,
    item_name  VARCHAR(100) NOT NULL,
    quantity   INT          NOT NULL,
    unit_price BIGINT       NOT NULL,
    CONSTRAINT ck_bill_line_quantity CHECK (quantity > 0),
    CONSTRAINT fk_bill_line_bill FOREIGN KEY (bill_id) REFERENCES bill (id),
    CONSTRAINT fk_bill_line_share FOREIGN KEY (share_id) REFERENCES bill_share (id)
);
CREATE INDEX ix_bill_line_bill ON bill_line (bill_id);

-- amount is what the guest handed over; change_amount is given back (cash only, BR-11).
-- The amount that counts towards the bill and the revenue is amount - change_amount.
CREATE TABLE payment (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    bill_id        BIGINT      NOT NULL,
    share_id       BIGINT,
    shift_id       BIGINT      NOT NULL,
    method         VARCHAR(20) NOT NULL,
    amount         BIGINT      NOT NULL,
    change_amount  BIGINT      NOT NULL DEFAULT 0,
    paid_at        DATETIME    NOT NULL,
    recorded_by_id BIGINT      NOT NULL,
    CONSTRAINT ck_payment_amount CHECK (amount > 0),
    CONSTRAINT ck_payment_change CHECK (change_amount >= 0 AND change_amount < amount),
    CONSTRAINT fk_payment_bill FOREIGN KEY (bill_id) REFERENCES bill (id),
    CONSTRAINT fk_payment_share FOREIGN KEY (share_id) REFERENCES bill_share (id),
    CONSTRAINT fk_payment_shift FOREIGN KEY (shift_id) REFERENCES shift (id),
    CONSTRAINT fk_payment_recorded_by FOREIGN KEY (recorded_by_id) REFERENCES users (id)
);
CREATE INDEX ix_payment_bill ON payment (bill_id);
CREATE INDEX ix_payment_shift ON payment (shift_id);
