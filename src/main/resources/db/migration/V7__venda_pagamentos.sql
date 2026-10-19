CREATE TABLE IF NOT EXISTS venda_pagamentos (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  venda_id BIGINT NOT NULL,
  forma_pagamento VARCHAR(20) NOT NULL,
  valor DECIMAL(10,2) NOT NULL,
  CONSTRAINT fk_pagamento_venda FOREIGN KEY (venda_id) REFERENCES vendas (id) ON DELETE CASCADE
);

CREATE INDEX idx_pagamento_venda ON venda_pagamentos (venda_id);
