# ADR-0001: TEF por interface abstrata com implementação simulada

## Status
Aceito

## Contexto
Precisávamos preparar o sistema para integração com maquininha TEF sem acoplar ao SDK da operadora neste momento.

## Decisão
Criar pacote `tef/` com interface `OperadoraCartao`, DTOs, `OperadoraCartaoFactory` e `OperadoraCartaoSimulada`. Apenas abstração + simulado nesta fase.

## Consequências
- **Positivas:** Isolamento, testabilidade, fácil plugar SDK real futuramente.
- **Negativas:** Nenhuma integração real agora (intencional).
