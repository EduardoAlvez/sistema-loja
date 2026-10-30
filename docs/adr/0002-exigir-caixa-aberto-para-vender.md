# ADR-0002: Exigir caixa aberto para registrar venda

## Status
Aceito

## Contexto
Era possível registrar venda sem caixa aberto, pois a validação estava apenas na UI.

## Decisão
Adicionar validação no `VendaService.registrar()` exigindo caixa aberto via `CaixaService`. A regra passa a ser obrigatória na camada de serviço.

## Consequências
- **Positivas:** Maior consistência, evita inconsistências no fechamento, protege contra chamadas diretas.
- **Negativas:** Testes/UI precisam garantir caixa aberto (já tratado).
