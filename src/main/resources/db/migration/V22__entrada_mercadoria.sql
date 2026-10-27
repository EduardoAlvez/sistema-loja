CREATE TABLE entrada_mercadoria (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  fornecedor_id BIGINT,
  numero_nota VARCHAR(30),
  data_entrada DATETIME NOT NULL,
  usuario_id BIGINT,
  valor_total DECIMAL(18,2) NOT NULL DEFAULT 0.00,
  observacao VARCHAR(255),
  criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_entrada_fornecedor FOREIGN KEY (fornecedor_id) REFERENCES fornecedores(id),
  CONSTRAINT fk_entrada_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);
CREATE INDEX idx_entrada_data ON entrada_mercadoria(data_entrada);

CREATE TABLE entrada_item (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  entrada_id BIGINT NOT NULL,
  produto_id BIGINT NOT NULL,
  quantidade INT NOT NULL,
  valor_unitario DECIMAL(18,6) NOT NULL,
  subtotal DECIMAL(18,2) NOT NULL,
  CONSTRAINT fk_entrada_item_entrada FOREIGN KEY (entrada_id) REFERENCES entrada_mercadoria(id) ON DELETE CASCADE,
  CONSTRAINT fk_entrada_item_produto FOREIGN KEY (produto_id) REFERENCES produtos(id)
);
CREATE INDEX idx_entrada_item_produto ON entrada_item(produto_id);
