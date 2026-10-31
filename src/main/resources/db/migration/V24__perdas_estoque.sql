CREATE TABLE perda_estoque (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  produto_id BIGINT NOT NULL,
  quantidade INT NOT NULL,
  motivo VARCHAR(20) NOT NULL,
  observacao VARCHAR(255),
  usuario_id BIGINT,
  data_hora TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_perda_produto FOREIGN KEY (produto_id) REFERENCES produto(id),
  CONSTRAINT fk_perda_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id)
);
