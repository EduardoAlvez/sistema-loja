ALTER TABLE clientes ADD COLUMN saldo_pontos INT NOT NULL DEFAULT 0;

ALTER TABLE vendas ADD COLUMN pontos_creditados INT NOT NULL DEFAULT 0;
ALTER TABLE vendas ADD COLUMN pontos_resgatados INT NOT NULL DEFAULT 0;
ALTER TABLE vendas ADD COLUMN valor_pontos DECIMAL(10,2) NOT NULL DEFAULT 0;

ALTER TABLE vendas_pausadas ADD COLUMN pontos_resgatados INT NOT NULL DEFAULT 0;
ALTER TABLE vendas_pausadas ADD COLUMN valor_pontos DECIMAL(10,2) NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS fidelidade_extrato (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  cliente_id BIGINT NOT NULL,
  venda_id BIGINT,
  devolucao_id BIGINT,
  tipo VARCHAR(20) NOT NULL,
  pontos INT NOT NULL,
  descricao VARCHAR(120),
  data_hora TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_fidelidade_cliente FOREIGN KEY (cliente_id) REFERENCES clientes (id)
);

CREATE INDEX idx_fidelidade_extrato_cliente ON fidelidade_extrato (cliente_id, id);
