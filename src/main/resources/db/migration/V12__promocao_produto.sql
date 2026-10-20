ALTER TABLE produtos ADD COLUMN preco_promocional DECIMAL(10,2) NULL;
ALTER TABLE produtos ADD COLUMN promo_inicio DATE NULL;
ALTER TABLE produtos ADD COLUMN promo_fim DATE NULL;
ALTER TABLE produtos ADD COLUMN promo_dias VARCHAR(20) NULL;
ALTER TABLE produtos ADD COLUMN promo_hora_inicio TIME NULL;
ALTER TABLE produtos ADD COLUMN promo_hora_fim TIME NULL;
