-- Order & Kitchen module, menu and order part (owner: Tang Thoai Lam)
-- Matches docs/diagrams/lam/04-erd-2-order-menu.png
-- The kitchen ticket tables (04-erd-3-kitchen-tickets.png) are added in a later migration.

CREATE TABLE menu_category (
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    CONSTRAINT uk_menu_category_name UNIQUE (name)
);

CREATE TABLE menu_item (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id BIGINT       NOT NULL,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(300),
    price       BIGINT       NOT NULL,
    course      VARCHAR(20)  NOT NULL,
    in_stock    BOOLEAN      NOT NULL DEFAULT TRUE,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_menu_item_category_name UNIQUE (category_id, name),
    CONSTRAINT ck_menu_item_price CHECK (price > 0),
    CONSTRAINT fk_menu_item_category FOREIGN KEY (category_id) REFERENCES menu_category (id)
);

CREATE TABLE customer_order (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    table_id   BIGINT      NOT NULL,
    waiter_id  BIGINT      NOT NULL,
    status     VARCHAR(20) NOT NULL,
    created_at DATETIME    NOT NULL,
    CONSTRAINT fk_customer_order_table FOREIGN KEY (table_id) REFERENCES dining_table (id),
    CONSTRAINT fk_customer_order_waiter FOREIGN KEY (waiter_id) REFERENCES users (id)
);
CREATE INDEX ix_customer_order_table_status ON customer_order (table_id, status);

CREATE TABLE order_line (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id     BIGINT       NOT NULL,
    menu_item_id BIGINT       NOT NULL,
    quantity     INT          NOT NULL,
    unit_price   BIGINT       NOT NULL,
    note         VARCHAR(200),
    CONSTRAINT ck_order_line_quantity CHECK (quantity BETWEEN 1 AND 50),
    CONSTRAINT fk_order_line_order FOREIGN KEY (order_id) REFERENCES customer_order (id),
    CONSTRAINT fk_order_line_menu_item FOREIGN KEY (menu_item_id) REFERENCES menu_item (id)
);
CREATE INDEX ix_order_line_order ON order_line (order_id);
