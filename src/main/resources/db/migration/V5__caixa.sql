CREATE TABLE IF NOT EXISTS caixas (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  data_abertura TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  data_fechamento TIMESTAMP,
  usuario_abertura_id BIGINT NOT NULL,
  usuario_fechamento_id BIGINT,
  valor_abertura DECIMAL(10,2) NOT NULL DEFAULT 0,
  valor_fechamento DECIMAL(10,2),
  status VARCHAR(20) NOT NULL DEFAULT 'ABERTO',
  observacao VARCHAR(255),
  CONSTRAINT fk_caixas_abertura FOREIGN KEY (usuario_abertura_id) REFERENCES usuarios (id),
  CONSTRAINT fk_caixas_fechamento FOREIGN KEY (usuario_fechamento_id) REFERENCES usuarios (id)
);

CREATE INDEX idx_caixas_status ON caixas (status);
