--liquibase formatted sql
--changeset maison-vigne:002
CREATE TABLE purchase_order (
    id BIGSERIAL PRIMARY KEY,
    wine_id BIGINT NOT NULL REFERENCES wine(id),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    ordered_date DATE NOT NULL,
    delivery_date DATE NOT NULL
);
CREATE INDEX idx_purchase_order_delivery_date ON purchase_order(delivery_date, id);