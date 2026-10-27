CREATE TABLE taxas_cartao (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  bandeira VARCHAR(20) NOT NULL,
  tipo_cartao VARCHAR(10) NOT NULL,
  parcelas_min INT NOT NULL DEFAULT 1,
  parcelas_max INT NOT NULL DEFAULT 1,
  taxa_percentual DECIMAL(6,4) NOT NULL DEFAULT 0.0000,
  taxa_fixa DECIMAL(18,6) NOT NULL DEFAULT 0.0000,
  adquirente VARCHAR(30),
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_taxas_cartao_bandeira_tipo ON taxas_cartao(bandeira, tipo_cartao, ativo);
