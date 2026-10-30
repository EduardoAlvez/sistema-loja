CREATE TABLE contagem_inventario (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  numero VARCHAR(30) NOT NULL UNIQUE,
  status VARCHAR(15) NOT NULL,
  usuario_id BIGINT NOT NULL,
  iniciado_em TIMESTAMP NOT NULL,
  fechado_em TIMESTAMP NULL,
  observacao VARCHAR(255),
  INDEX idx_contagem_status (status)
);
CREATE TABLE contagem_item (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  contagem_id BIGINT NOT NULL,
  produto_id BIGINT NOT NULL,
  estoque_sistema INT NOT NULL,
  quantidade_contada INT NOT NULL DEFAULT 0,
  diferenca INT NOT NULL DEFAULT 0,
  ajustado BOOLEAN NOT NULL DEFAULT FALSE,
  CONSTRAINT fk_contagem_item_contagem FOREIGN KEY (contagem_id) REFERENCES contagem_inventario(id),
  CONSTRAINT fk_contagem_item_produto FOREIGN KEY (produto_id) REFERENCES produtos(id),
  UNIQUE KEY uk_contagem_produto (contagem_id, produto_id)
);
CREATE TABLE perda_estoque (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  produto_id BIGINT NOT NULL,
  quantidade INT NOT NULL,
  motivo VARCHAR(30) NOT NULL,
  usuario_id BIGINT NOT NULL,
  observacao VARCHAR(255),
  data_hora TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_perda_produto FOREIGN KEY (produto_id) REFERENCES produtos(id),
  CONSTRAINT fk_perda_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);
