CREATE TABLE IF NOT EXISTS caixa_lancamentos (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  caixa_id BIGINT NOT NULL,
  data_hora TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  tipo VARCHAR(20) NOT NULL,
  valor DECIMAL(10,2) NOT NULL,
  usuario_id BIGINT,
  observacao VARCHAR(255),
  CONSTRAINT fk_lancamento_caixa FOREIGN KEY (caixa_id) REFERENCES caixas (id) ON DELETE CASCADE,
  CONSTRAINT fk_lancamento_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
);

CREATE INDEX idx_lancamento_caixa ON caixa_lancamentos (caixa_id);
