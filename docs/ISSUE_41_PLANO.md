# Plano Issue #41 - Inventário (contagem cíclica + perdas com motivo)

## Regras
- Opcional: não obrigatório para vender nem fechar caixa
- Visível apenas para GERENTE ou ADMIN
- Apenas aditivo (novas tabelas/migrações)
- Toda ação registrada em auditoria

## Schema (migrações)
- V24__inventario_contagem.sql
  - contagem_inventario(id, numero, status(ABERTA/FECHADA/CANCELADA), usuario_id, iniciado_em, fechado_em, observacao)
  - contagem_item(id, contagem_id, produto_id, estoque_sistema, quantidade_contada, diferenca, ajustado)
- V25__perdas_estoque.sql
  - perda_estoque(id, produto_id, quantidade, motivo, usuario_id, observacao, data_hora)

## Camadas
- model: ContagemInventario, ContagemItem, PerdaEstoque
- repository: ContagemInventarioRepository, PerdaEstoqueRepository
- service: InventarioService (abrir contagem, lancar contagem, fechar com ajustes, registrar perda)
- ui: PainelInventario, DialogoContagemItem, DialogoRegistrarPerda
- Aplicacao: wiring + getters

## Menu
- TelaPrincipal: adicionar menu "Inventário" (visível apenas GERENTE/ADMIN), item "Contagem cíclica" e "Registrar perda"

## Commits pequenos
1. feat(db): migracoes V24/V25 inventario e perdas
2. feat(model): modelos ContagemInventario/Item e PerdaEstoque
3. feat(repo): repositórios inventario e perdas
4. feat(service): InventarioService com regras e auditoria
5. feat(ui): PainelInventario e diálogos
6. feat(ui): adiciona menu Inventario para GERENTE/ADMIN
7. test: testes básicos do InventarioService
8. docs: atualiza README/docs
