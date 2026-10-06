CREATE TABLE IF NOT EXISTS usuarios (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  nome VARCHAR(120) NOT NULL,
  login VARCHAR(60) NOT NULL,
  senha_hash VARCHAR(128) NOT NULL,
  sal VARCHAR(64) NOT NULL,
  perfil VARCHAR(20) NOT NULL,
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT uk_usuarios_login UNIQUE (login)
);

CREATE TABLE IF NOT EXISTS categorias (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  nome VARCHAR(80) NOT NULL,
  CONSTRAINT uk_categorias_nome UNIQUE (nome)
);

CREATE TABLE IF NOT EXISTS produtos (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  codigo_barras VARCHAR(40) NOT NULL,
  nome VARCHAR(120) NOT NULL,
  categoria_id BIGINT,
  preco_custo DECIMAL(10,2) NOT NULL DEFAULT 0,
  preco_venda DECIMAL(10,2) NOT NULL,
  estoque INT NOT NULL DEFAULT 0,
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT fk_produtos_categoria FOREIGN KEY (categoria_id) REFERENCES categorias (id),
  CONSTRAINT uk_produtos_codigo UNIQUE (codigo_barras)
);

CREATE TABLE IF NOT EXISTS clientes (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  nome VARCHAR(120) NOT NULL,
  cpf VARCHAR(14) NOT NULL,
  telefone VARCHAR(20),
  email VARCHAR(120),
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT uk_clientes_cpf UNIQUE (cpf)
);

CREATE TABLE IF NOT EXISTS vendas (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  numero VARCHAR(20) NOT NULL,
  data_hora TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  cliente_id BIGINT,
  usuario_id BIGINT,
  forma_pagamento VARCHAR(20) NOT NULL,
  total DECIMAL(10,2) NOT NULL,
  valor_pago DECIMAL(10,2) NOT NULL,
  troco DECIMAL(10,2) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'CONCLUIDA',
  CONSTRAINT uk_vendas_numero UNIQUE (numero),
  CONSTRAINT fk_vendas_cliente FOREIGN KEY (cliente_id) REFERENCES clientes (id),
  CONSTRAINT fk_vendas_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
);

CREATE TABLE IF NOT EXISTS venda_itens (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  venda_id BIGINT NOT NULL,
  produto_id BIGINT NOT NULL,
  quantidade INT NOT NULL,
  preco_unitario DECIMAL(10,2) NOT NULL,
  subtotal DECIMAL(10,2) NOT NULL,
  CONSTRAINT fk_itens_venda FOREIGN KEY (venda_id) REFERENCES vendas (id) ON DELETE CASCADE,
  CONSTRAINT fk_itens_produto FOREIGN KEY (produto_id) REFERENCES produtos (id)
);
