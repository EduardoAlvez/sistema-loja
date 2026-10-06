CREATE TABLE IF NOT EXISTS auditoria (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  data_hora TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  usuario_login VARCHAR(60) NOT NULL,
  acao VARCHAR(40) NOT NULL,
  entidade VARCHAR(40),
  entidade_id BIGINT,
  detalhe VARCHAR(255)
);

CREATE INDEX idx_auditoria_data_hora ON auditoria (data_hora);
