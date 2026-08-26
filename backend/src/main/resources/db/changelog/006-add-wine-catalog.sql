--liquibase formatted sql
--changeset maison-vigne:006
INSERT INTO wine (name, region, variety, vintage, category, image, description, price, stock, threshold) VALUES
('Luna Nera', 'Napa Valley', 'Merlot', '2019', 'Red', '/assets/wines/luna-nera.jpg', 'プラムとベリーの濃密な香りに、甘いスパイスと柔らかな木のニュアンスが広がります。口当たりはまろやかで余韻が長い一品です。', 6800.0, 27, 9),
('Rosso del Fiume', 'Veneto', 'Corvina', '2020', 'Red', '/assets/wines/rosso-del-fiume.jpg', 'チェリーやラズベリーを思わせる果実感と、花のような軽やかさが調和した優雅な赤ワインです。', 5200.0, 31, 8),
('Alba Crest', 'Barolo', 'Nebbiolo', '2016', 'Red', '/assets/wines/alba-crest.jpg', '華やかな花の香りと、土の深みが混ざり合う複雑な味わい。力強い骨格が食事とよく合います。', 9100.0, 16, 4),
('Jade Nocturne', 'Loire Valley', 'Chenin Blanc', '2022', 'White', '/assets/wines/jade-nocturne.jpg', '洋梨とアカシアの香りが広がり、活きた酸が口の中を軽やかに整えます。上品な余韻が魅力です。', 4800.0, 28, 9),
('Bellavista Gold', 'Friuli', 'Pinot Grigio', '2021', 'White', '/assets/wines/bellavista-gold.jpg', '柑橘類の鮮やかさとミネラル感が際立ち、すっきりとした酸味で食前の一杯に最適です。', 3900.0, 34, 11),
('Hoshizora Reserve', 'Yamanashi', 'Cabernet Franc', '2018', 'Red', '/assets/wines/hoshizora-reserve.jpg', '黒い果実とスミレの香りが持ち上がり、柔らかなタンニンが長く続く落ち着いた印象のワインです。', 7600.0, 18, 6),
('Cedar Valley', 'Sonoma', 'Syrah', '2020', 'Red', '/assets/wines/cedar-valley.jpg', 'ブラックベリーとスモーキーな香りが感じられ、熟した果実とスパイスのバランスが見事です。', 6900.0, 21, 7),
('Golden Orchard', 'Alsace', 'Riesling', '2023', 'White', '/assets/wines/golden-orchard.jpg', '青リンゴと花の香りに、細かな酸味と甘みがほどよく重なり、鮮やかな印象を残します。', 4100.0, 38, 12),
('Aurora Solare', 'Prosecco', 'Glera', '2022', 'Sparkling', '/assets/wines/aurora-solare.jpg', '軽快な泡立ちと白い花の香りが心地よく、乾いた口当たりで食事を引き立てます。', 3600.0, 45, 14),
('Mistral Terrace', 'South Australia', 'Shiraz', '2019', 'Red', '/assets/wines/mistral-terrace.jpg', 'ブラックチェリーとペッパーの香りが際立ち、深みのある果実味と余韻の長さが特徴です。', 6100.0, 25, 8),
('Eucalyptus Bloom', 'Melbourne', 'Viognier', '2021', 'White', '/assets/wines/eucalyptus-bloom.jpg', '桃とハチミツの甘く柔らかな香りに、クリーミーな口当たりが加わり、華やかな飲み心地です。', 4700.0, 29, 9),
('Northwind Select', 'Willamette', 'Pinot Noir', '2020', 'Red', '/assets/wines/northwind-select.jpg', '赤い果実とスパイスの香りが繊細に広がり、酸とタンニンがほどよく整った味わいです。', 5800.0, 24, 7),
('Puro Cielo', 'Sicily', 'Nero Avola', '2021', 'Red', '/assets/wines/puro-cielo.jpg', '熟したプラムと黒胡椒の香りが描く、深い色合いと豊かな果実味が魅力の一本です。', 5500.0, 32, 10),
('Finca de Piedra', 'Spain', 'Tempranillo', '2019', 'Red', '/assets/wines/finca-de-piedra.jpg', '赤いベリーの香りとスモーキーな余韻が続く、食卓を華やかに彩る上品な赤ワインです。', 6300.0, 26, 8);