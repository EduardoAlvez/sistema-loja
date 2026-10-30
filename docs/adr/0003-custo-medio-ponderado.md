# ADR-0003: Usar custo médio ponderado na entrada de mercadoria

## Status
Aceito

## Contexto
Necessário manter custo correto ao receber mercadorias com preços diferentes ao longo do tempo.

## Decisão
Calcular custo médio ponderado na entrada: `custoMedioNovo = (estoqueAtual * custoMedioAtual + qtdEntrada * valorUnitario) / (estoqueAtual + qtdEntrada)`. Operação em transação.

## Consequências
- **Positivas:** Custo mais realista, base para relatórios e sugestão de compra.
- **Negativas:** Requer cuidado com transações e integridade.
