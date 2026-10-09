-- Seed 100 products once (Flyway runs each migration exactly once). Values are deterministic;
-- every 7th product is out of stock so the in-stock filter has something to exclude.
INSERT INTO product (name, category, price, stock, rating, created_at, updated_at)
SELECT 'Product ' || lpad(i::text, 3, '0'),
       (ARRAY ['Electronics', 'Books', 'Clothing', 'Home', 'Sports'])[1 + i % 5],
       ((i * 37) % 500 + 5) + 0.99,
       CASE WHEN i % 7 = 0 THEN 0 ELSE (i * 13) % 50 + 1 END,
       ((i * 17) % 51) / 10.0,
       now() - (i || ' hours')::interval,
       now() - (i || ' hours')::interval
FROM generate_series(1, 100) AS s(i);
