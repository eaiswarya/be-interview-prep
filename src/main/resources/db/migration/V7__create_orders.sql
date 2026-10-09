-- "order" is a reserved word in SQL, hence orders.
CREATE TABLE orders (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT       NOT NULL REFERENCES app_user (id),
    idempotency_key VARCHAR(100) NOT NULL,
    status          VARCHAR(20)  NOT NULL CHECK (status IN ('PLACED', 'CANCELLED')),
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    -- The arbiter for retries: one order per (user, key), even for simultaneous duplicates.
    CONSTRAINT uq_orders_user_idempotency_key UNIQUE (user_id, idempotency_key)
);

CREATE TABLE order_item (
    id         BIGSERIAL PRIMARY KEY,
    order_id   BIGINT  NOT NULL REFERENCES orders (id),
    product_id BIGINT  NOT NULL REFERENCES product (id),
    quantity   INTEGER NOT NULL CHECK (quantity > 0)
);

CREATE INDEX idx_order_item_order ON order_item (order_id);
