CREATE TABLE product (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(100)  NOT NULL,
    category   VARCHAR(50)   NOT NULL,
    price      NUMERIC(10, 2) NOT NULL CHECK (price >= 0),
    stock      INTEGER       NOT NULL CHECK (stock >= 0),
    rating     NUMERIC(2, 1) NOT NULL CHECK (rating BETWEEN 0 AND 5),
    created_at TIMESTAMPTZ   NOT NULL,
    updated_at TIMESTAMPTZ   NOT NULL
);

-- Indexes for the list filters; the trigram index makes "name contains" searches index-backed.
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX idx_product_category ON product (category);
CREATE INDEX idx_product_price ON product (price);
CREATE INDEX idx_product_name_trgm ON product USING gin (lower(name) gin_trgm_ops);
