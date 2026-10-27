CREATE TABLE fornecedores (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  nome VARCHAR(100) NOT NULL,
  cnpj VARCHAR(18),
  telefone VARCHAR(20),
  email VARCHAR(100),
  endereco VARCHAR(150),
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_fornecedores_nome ON fornecedores(nome);
