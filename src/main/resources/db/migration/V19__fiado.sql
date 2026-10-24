ALTER TABLE clientes ADD COLUMN IF NOT EXISTS limite_credito DECIMAL(18,6) NOT NULL DEFAULT 0;

ALTER TABLE configuracoes ADD COLUMN IF NOT EXISTS fiado_habilitado BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE configuracoes ADD COLUMN IF NOT EXISTS taxa_juros_fiado DECIMAL(5,4) NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS fiados (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  venda_id BIGINT NOT NULL,
  cliente_id BIGINT NOT NULL,
  valor_total DECIMAL(18,6) NOT NULL,
  valor_pago DECIMAL(18,6) NOT NULL DEFAULT 0,
  valor_devedor DECIMAL(18,6) NOT NULL,
  juros_aplicados DECIMAL(18,6) NOT NULL DEFAULT 0,
  data_vencimento DATE NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ABERTO',
  observacao VARCHAR(255),
  CONSTRAINT fk_fiados_venda FOREIGN KEY (venda_id) REFERENCES vendas(id),
  CONSTRAINT fk_fiados_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id)
);

CREATE INDEX idx_fiados_cliente ON fiados (cliente_id);
CREATE INDEX idx_fiados_status ON fiados (status);
CREATE INDEX idx_fiados_vencimento ON fiados (data_vencimento);

CREATE TABLE IF NOT EXISTS recebimentos_fiado (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  fiado_id BIGINT NOT NULL,
  data_pagamento TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  valor DECIMAL(18,6) NOT NULL,
  forma_pagamento VARCHAR(20) NOT NULL,
  usuario_id BIGINT NOT NULL,
  observacao VARCHAR(255),
  CONSTRAINT fk_recebimentos_fiado FOREIGN KEY (fiado_id) REFERENCES fiados(id),
  CONSTRAINT fk_recebimentos_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

CREATE INDEX idx_recebimentos_fiado ON recebimentos_fiado (fiado_id);
