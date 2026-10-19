ALTER TABLE clientes ADD COLUMN credito DECIMAL(10,2) NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS devolucoes (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  venda_id BIGINT NOT NULL,
  cliente_id BIGINT,
  usuario_id BIGINT,
  data_hora TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  valor DECIMAL(10,2) NOT NULL,
  credito_cliente BOOLEAN NOT NULL DEFAULT FALSE,
  observacao VARCHAR(255),
  CONSTRAINT fk_devolucao_venda FOREIGN KEY (venda_id) REFERENCES vendas (id),
  CONSTRAINT fk_devolucao_cliente FOREIGN KEY (cliente_id) REFERENCES clientes (id),
  CONSTRAINT fk_devolucao_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
);

CREATE INDEX idx_devolucao_venda ON devolucoes (venda_id);

CREATE TABLE IF NOT EXISTS devolucao_itens (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  devolucao_id BIGINT NOT NULL,
  venda_item_id BIGINT NOT NULL,
  produto_id BIGINT NOT NULL,
  produto_nome VARCHAR(150) NOT NULL,
  quantidade INT NOT NULL,
  valor_unitario DECIMAL(10,2) NOT NULL,
  CONSTRAINT fk_devolucao_item FOREIGN KEY (devolucao_id) REFERENCES devolucoes (id) ON DELETE CASCADE,
  CONSTRAINT fk_devolucao_item_produto FOREIGN KEY (produto_id) REFERENCES produtos (id)
);

CREATE INDEX idx_devolucao_item ON devolucao_itens (venda_item_id);
