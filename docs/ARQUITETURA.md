# Arquitetura - Sistema Loja

## Camadas
O projeto segue o padrão **package-by-layer**:

- `model/` - Entidades e enums
- `repository/` - Acesso a dados (JDBC/SQL). Migrações via Flyway
- `service/` - Regras de negócio e orquestração
- `ui/` - Interface Swing (painéis, diálogos)
- `db/` - Conexão e configuração
- `impressao/` - Impressão de cupom
- `grafico/` - Gráficos
- `tef/` - Abstração TEF (interface + simulado)

## Aplicacao (Wiring)
`Aplicacao` centraliza criação de repositórios, serviços e dependências. É o ponto único para injeção manual.

## Migrações
Arquivos em `src/main/resources/db/migration/` com Flyway (`V1__...`, `V23__taxas_cartao.sql`, etc.). Suportam MySQL e H2 em testes.

## Fluxo principal (PDV)
1. UI (`PainelPDV`) monta `Venda` com itens/pagamentos
2. `VendaService.registrar()` valida (caixa aberto, itens, formas, TEF para cartão)
3. Persiste via `VendaRepository`
4. Atualiza estoque, registra auditoria
5. Integra com fidelidade/promocoções/autorização quando aplicável

## Configuração
- `docker-compose.yml` com MySQL 8.4 LTS
- Testes usam H2 em memória
