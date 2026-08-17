--liquibase formatted sql
--changeset maison-vigne:005
ALTER TABLE wine ADD COLUMN description VARCHAR(2000);

UPDATE wine SET description = CASE name
    WHEN 'Château Lueur Noire' THEN '黒系果実の凝縮感に、カカオと杉の香りが重なる重厚な一本。きめ細かなタンニンが長い余韻を形づくります。'
    WHEN 'Monteluna Estate' THEN '熟したチェリーとドライハーブの香り。伸びやかな酸と穏やかな樽香が、トスカーナらしい温かみを伝えます。'
    WHEN 'Aotearoa Cellars' THEN '瑞々しい柑橘とハーブの香りに凛とした酸が重なり、ミネラルを感じる端正な余韻へと続きます。'
    WHEN 'Valle di Sera' THEN '薔薇や赤い果実の香りに、土とスパイスの複雑さ。力強い骨格がほどけながら、優雅な余韻を残します。'
    WHEN 'Moonlight Spark' THEN '繊細な泡と白い花、焼きたてのブリオッシュの香り。乾杯から食事の終わりまで寄り添う上品な味わいです。'
    WHEN 'Sakura Reserve' THEN '白桃と和柑橘の繊細な香り。甲州らしい清らかな酸とほのかな苦みが、料理を引き立てます。'
    ELSE description
END;