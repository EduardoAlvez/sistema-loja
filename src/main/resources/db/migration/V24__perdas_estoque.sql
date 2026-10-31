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
