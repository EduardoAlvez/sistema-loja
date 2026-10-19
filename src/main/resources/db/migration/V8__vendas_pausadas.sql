CREATE TABLE IF NOT EXISTS vendas_pausadas (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  data_hora TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  cliente_id BIGINT,
  usuario_id BIGINT,
  forma_pagamento VARCHAR(20),
  desconto DECIMAL(10,2) NOT NULL DEFAULT 0,
  total DECIMAL(10,2) NOT NULL DEFAULT 0,
  observacao VARCHAR(255),
  CONSTRAINT fk_pausada_cliente FOREIGN KEY (cliente_id) REFERENCES clientes (id),
  CONSTRAINT fk_pausada_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
);

CREATE TABLE IF NOT EXISTS venda_pausada_itens (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  pausada_id BIGINT NOT NULL,
  produto_id BIGINT NOT NULL,
  quantidade INT NOT NULL,
  preco_unitario DECIMAL(10,2) NOT NULL,
  desconto DECIMAL(10,2) NOT NULL DEFAULT 0,
  CONSTRAINT fk_pausada_item FOREIGN KEY (pausada_id) REFERENCES vendas_pausadas (id) ON DELETE CASCADE,
  CONSTRAINT fk_pausada_produto FOREIGN KEY (produto_id) REFERENCES produtos (id)
);

CREATE TABLE IF NOT EXISTS venda_pausada_pagamentos (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  pausada_id BIGINT NOT NULL,
  forma_pagamento VARCHAR(20) NOT NULL,
  valor DECIMAL(10,2) NOT NULL,
  CONSTRAINT fk_pausada_pagamento FOREIGN KEY (pausada_id) REFERENCES vendas_pausadas (id) ON DELETE CASCADE
);
