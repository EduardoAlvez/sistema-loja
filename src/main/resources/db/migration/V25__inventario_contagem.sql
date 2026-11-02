CREATE TABLE contagem_inventario (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  numero VARCHAR(24),
  status VARCHAR(20) NOT NULL DEFAULT 'ABERTA',
  usuario_id BIGINT NOT NULL,
  observacao VARCHAR(255),
  motivo_cancelamento VARCHAR(255),
  data_hora TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  data_fechamento TIMESTAMP,
  CONSTRAINT uk_contagem_inventario_numero UNIQUE (numero),
  CONSTRAINT ck_contagem_inventario_status CHECK (status IN ('ABERTA', 'FECHADA', 'CANCELADA')),
  CONSTRAINT fk_contagem_inventario_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);
CREATE INDEX idx_contagem_inventario_status ON contagem_inventario(status);

CREATE TABLE contagem_item (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  contagem_id BIGINT NOT NULL,
  produto_id BIGINT NOT NULL,
  estoque_sistema INT NOT NULL,
  quantidade_contada INT,
  delta_aplicado INT,
  ajustado BOOLEAN NOT NULL DEFAULT FALSE,
  CONSTRAINT uk_contagem_item UNIQUE (contagem_id, produto_id),
  CONSTRAINT fk_contagem_item_contagem FOREIGN KEY (contagem_id) REFERENCES contagem_inventario(id) ON DELETE CASCADE,
  CONSTRAINT fk_contagem_item_produto FOREIGN KEY (produto_id) REFERENCES produtos(id)
);
CREATE INDEX idx_contagem_item_produto ON contagem_item(produto_id);

CREATE TABLE contagem_controle (
  id INT PRIMARY KEY
);
INSERT INTO contagem_controle (id) VALUES (1);
