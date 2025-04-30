CREATE TABLE IF NOT EXISTS product_entity
(
    id       UUID PRIMARY KEY,
    name     VARCHAR(255) NOT NULL,
    quantity INT          NOT NULL
);
