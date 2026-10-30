# ADR-0004: Manter arquitetura package-by-layer

## Status
Aceito

## Contexto
Avaliado migrar para package-by-feature. O projeto já está estável no padrão atual.

## Decisão
Manter `model/service/repository/ui/db/impressao/grafico/tef` por enquanto. Não migrar agora.

## Consequências
- **Positivas:** Menor risco, padrões existentes preservados, refatoração adiada.
- **Negativas:** Pode crescer menos coeso em features muito grandes no futuro.
