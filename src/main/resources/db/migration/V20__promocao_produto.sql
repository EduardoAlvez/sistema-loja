CREATE TABLE IF NOT EXISTS promocao_produto (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    produto_id BIGINT NOT NULL,
    nome VARCHAR(100) NOT NULL,
    quantidade_minima INT NOT NULL,
    preco_promocional DECIMAL(18,6) NOT NULL,
    ativa BOOLEAN NOT NULL DEFAULT TRUE,
    data_inicio DATE,
    data_fim DATE,
    dias_semana VARCHAR(20),
    horario_inicio TIME,
    horario_fim TIME,
    CONSTRAINT fk_promocao_produto_produto FOREIGN KEY (produto_id) REFERENCES produtos(id)
);

CREATE INDEX idx_promocao_produto_produto_id ON promocao_produto(produto_id);
CREATE INDEX idx_promocao_produto_ativa ON promocao_produto(ativa);
