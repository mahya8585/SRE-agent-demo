--liquibase formatted sql
--changeset maison-vigne:001
CREATE TABLE wine (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    region VARCHAR(255), variety VARCHAR(255), vintage VARCHAR(32), category VARCHAR(64), image VARCHAR(512),
    price DOUBLE PRECISION NOT NULL, stock INTEGER NOT NULL, threshold INTEGER NOT NULL
);
CREATE TABLE customer_order (
    id BIGSERIAL PRIMARY KEY,
    order_number VARCHAR(32) NOT NULL UNIQUE, status VARCHAR(32) NOT NULL,
    customer_name VARCHAR(255) NOT NULL, email VARCHAR(255) NOT NULL, delivery_address VARCHAR(1000) NOT NULL,
    item_count INTEGER NOT NULL, subtotal DOUBLE PRECISION NOT NULL, shipping DOUBLE PRECISION NOT NULL,
    total DOUBLE PRECISION NOT NULL, created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE TABLE order_line (
    id BIGSERIAL PRIMARY KEY, order_id BIGINT NOT NULL REFERENCES customer_order(id),
    wine_id BIGINT NOT NULL REFERENCES wine(id), wine_name VARCHAR(255) NOT NULL,
    unit_price DOUBLE PRECISION NOT NULL, quantity INTEGER NOT NULL, line_total DOUBLE PRECISION NOT NULL
);
CREATE INDEX idx_order_line_order_id ON order_line(order_id);
INSERT INTO wine (name, region, variety, vintage, category, image, price, stock, threshold) VALUES
('Château Lueur Noire', 'Bordeaux', 'Cabernet Sauvignon', '2018', 'Red', '/assets/wines/chateau-lueur-noire.jpg', 7400.0, 24, 8),
('Monteluna Estate', 'Tuscany', 'Sangiovese', '2019', 'Red', '/assets/wines/monteluna-estate.jpg', 4600.0, 19, 6),
('Aotearoa Cellars', 'Marlborough', 'Sauvignon Blanc', '2021', 'White', '/assets/wines/aotearoa-cellars.jpg', 3700.0, 35, 10),
('Valle di Sera', 'Piedmont', 'Nebbiolo', '2017', 'Red', '/assets/wines/valle-di-sera.jpg', 8300.0, 14, 5),
('Moonlight Spark', 'Champagne', 'Champagne Blend', '2020', 'Sparkling', '/assets/wines/moonlight-spark.jpg', 5900.0, 22, 7),
('Sakura Reserve', 'Yamanashi', 'Koshu', '2020', 'White', '/assets/wines/sakura-reserve.jpg', 4300.0, 12, 4);