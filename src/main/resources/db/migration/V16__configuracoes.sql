CREATE TABLE IF NOT EXISTS configuracoes (
  id BIGINT PRIMARY KEY,
  pontos_por_real DECIMAL(18,6) NOT NULL,
  valor_por_ponto DECIMAL(18,6) NOT NULL,
  pix_chave VARCHAR(77) NOT NULL,
  pix_nome VARCHAR(25) NOT NULL,
  pix_cidade VARCHAR(15) NOT NULL,
  atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT chk_configuracoes_id CHECK (id = 1),
  CONSTRAINT chk_configuracoes_pontos_por_real_pos CHECK (pontos_por_real > 0),
  CONSTRAINT chk_configuracoes_valor_por_ponto_pos CHECK (valor_por_ponto > 0),
  CONSTRAINT chk_configuracoes_fidelidade_produto CHECK (pontos_por_real * valor_por_ponto <= 1)
);

INSERT INTO configuracoes (id, pontos_por_real, valor_por_ponto, pix_chave, pix_nome, pix_cidade)
SELECT 1, 1.000000, 0.050000, 'pagamentos@lojaexemplo.com.br', 'LOJA EXEMPLO LTDA', 'SAO PAULO'
WHERE NOT EXISTS (SELECT 1 FROM configuracoes WHERE id = 1);
