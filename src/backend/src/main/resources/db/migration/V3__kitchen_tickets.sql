-- Order & Kitchen module, kitchen ticket part (owner: Tang Thoai Lam)
-- Matches docs/diagrams/lam/04-erd-3-kitchen-tickets.png
-- Billing & Reporting (Khoa) continues with V4.

CREATE TABLE kitchen_ticket (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT   NOT NULL,
    sent_at  DATETIME NOT NULL,
    CONSTRAINT fk_kitchen_ticket_order FOREIGN KEY (order_id) REFERENCES customer_order (id)
);
CREATE INDEX ix_kitchen_ticket_order ON kitchen_ticket (order_id);
CREATE INDEX ix_kitchen_ticket_sent_at ON kitchen_ticket (sent_at);

-- One ticket item per order line: the unique key is what stops a line from being sent twice (NFR-07).
-- version is for optimistic locking, so a waiter cancelling and a cook starting the same dish at the
-- same moment cannot both win (BR-08).
CREATE TABLE kitchen_ticket_item (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id     BIGINT      NOT NULL,
    order_line_id BIGINT      NOT NULL,
    status        VARCHAR(20) NOT NULL,
    version       BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT uk_kitchen_ticket_item_order_line UNIQUE (order_line_id),
    CONSTRAINT fk_kitchen_ticket_item_ticket FOREIGN KEY (ticket_id) REFERENCES kitchen_ticket (id),
    CONSTRAINT fk_kitchen_ticket_item_order_line FOREIGN KEY (order_line_id) REFERENCES order_line (id)
);
CREATE INDEX ix_kitchen_ticket_item_status ON kitchen_ticket_item (status);

CREATE TABLE item_status_change (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_item_id BIGINT      NOT NULL,
    status         VARCHAR(20) NOT NULL,
    changed_at     DATETIME    NOT NULL,
    reason         VARCHAR(200),
    CONSTRAINT fk_item_status_change_item FOREIGN KEY (ticket_item_id) REFERENCES kitchen_ticket_item (id)
);
CREATE INDEX ix_item_status_change_item ON item_status_change (ticket_item_id);
