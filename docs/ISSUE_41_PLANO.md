# Plano Issue #41 - Inventário (contagem cíclica + perdas com motivo)

## Status
**Fase 1 (perdas com motivo) — CONCLUÍDA em 31/10/2026.**
Fase 2 (contagem cíclica) — planejada, ainda não iniciada.

## Regras
- Opcional: não obrigatório para vender nem fechar caixa
- Visível apenas para GERENTE ou ADMIN
- Apenas aditivo (novas tabelas/migrações)
- Toda ação registrada em auditoria

## Schema (migrações)
- V24__perdas_estoque.sql
  - perda_estoque(id, produto_id, quantidade, motivo, usuario_id, observacao, data_hora)
- V25__inventario_contagem.sql (**Fase 2, ainda não criada**)
  - contagem_inventario(id, numero, status, usuario_id, iniciado_em, fechado_em, observacao)
  - contagem_item(id, contagem_id, produto_id, estoque_sistema, quantidade_contada, diferenca, ajustado)

> Atenção: tabelas reais são no plural (`produtos`, `usuarios`). Uma migração por versão.
> As tabelas de contagem foram removidas do schema na Fase 1 para não deixar schema morto:
> a Fase 2 cria a `V25` com o schema definitivo.

## Camadas
- model: ContagemInventario, ContagemItem, PerdaEstoque
- repository: ContagemInventarioRepository, PerdaEstoqueRepository
- service: InventarioService (abrir contagem, lancar contagem, fechar com ajustes, registrar perda)
- ui: PainelInventario, DialogoContagemItem, DialogoRegistrarPerda
- Aplicacao: wiring + getters

## Menu
- TelaPrincipal: menu "Gerência" > "Inventário" (visível apenas GERENTE/ADMIN), aba "Inventário"

## Commits pequenos
1. feat(db): migracoes V24/V25 inventario e perdas
2. feat(model): modelos ContagemInventario/Item e PerdaEstoque
3. feat(repo): repositórios inventario e perdas
4. feat(service): InventarioService com regras e auditoria
5. feat(ui): PainelInventario e diálogos
6. feat(ui): adiciona menu Inventario para GERENTE/ADMIN
7. test: testes básicos do InventarioService
8. docs: atualiza README/docs

---

## Fase 1 - Perdas com motivo (implementada)

### Entregue
- [x] `V25__perdas_estoque.sql` com `perda_estoque(produto_id, quantidade, motivo, observacao, usuario_id, data_hora)`
- [x] `model/PerdaEstoque.java`
- [x] `repository/PerdaEstoqueRepository.java` (`inserir` transacional, `listarPorProduto`)
- [x] `service/InventarioService.java`
  - [x] Permissão `usuario.isGerenteOuAdmin()`
  - [x] Quantidade > 0
  - [x] Motivo na lista `QUEBRA, VENCIMENTO, ROUBO, AVARIA, OUTROS` (normalizado para maiúsculas)
  - [x] Produto existe e estoque suficiente
  - [x] Transação: baixa de estoque + registro da perda + auditoria, com rollback em falha
  - [x] Auditoria `REGISTRAR_PERDA_ESTOQUE`
- [x] `ui/PainelInventario.java` (seleção de produto, quantidade, motivo, observação, histórico)
- [x] `ui/TelaPrincipal.java`: aba "Inventário" e menu "Gerência" > "Inventário" (GERENTE/ADMIN)
- [x] `Aplicacao`: wiring + `getInventario()`
- [x] `InventarioServiceTest` com 17 testes (permissão, validações, baixa de estoque, auditoria,
      limite da observação, OUTROS sem observação, responsável no histórico)
- [x] `PainelInventarioTest` com 5 testes (estado vazio, histórico, nome acessível dos campos,
      design system da tabela)
- [x] 351 testes verdes no total

### Correções aplicadas após revisão por agentes (31/10/2026)
- [x] Auditoria gravada **na mesma transação** da baixa de estoque (overloads transacionais em
      `AuditoriaRepository` e `AuditoriaService`)
- [x] Limite de 255 caracteres na observação, validado no serviço e limitado na digitação
- [x] `rollbackSilencioso` com `addSuppressed`; causa original preservada em `ValidacaoException`
- [x] Autorização pela **sessão** (com checagem de `ativo`), em vez de `Usuario` vindo do chamador
- [x] `enum MotivoPerda` como fonte única; `OUTROS` exige observação
- [x] Campos com `setLabelFor` (nome acessível), confirmação antes de registrar,
      `catch (RuntimeException)` em todos os caminhos e `Ui.modeloTabela`/`Ui.tabela`
- [x] Histórico exibe o **responsável** (JOIN em `usuarios`) e estado vazio na tabela

### Motivos de perda
| Motivo | Uso |
|--------|-----|
| QUEBRA | Produto quebrado ou avariado no estoque |
| VENCIMENTO | Produto com prazo de validade vencido |
| ROUBO | Furto ou perda por sinistro |
| AVARIA | Danos por transporte, umidade, temperatura |
| OUTROS | Casos não cobertos acima (exige observação) |

## Fase 2 - Contagem cíclica (pendente)

### Planejado
1. `model/ContagemInventario.java` e `model/ContagemItem.java`
2. `repository/ContagemInventarioRepository.java` (abrir, registrar contagem, fechar)
3. `InventarioService`: `abrirContagem`, `informarContagem`, `fecharContagem`
   - ajustes de estoque aplicados **somente após confirmação**, em transação única
4. `ui/DialogoContagemItem.java` e integração no `PainelInventario`
5. Testes: abertura, contagem, divergência, ajuste aplicado e rollback

### Regra crítica
Nunca ajustar estoque no momento da digitação da contagem. A contagem **registra**; o
fechamento **aplica** o ajuste. Sem isso, um erro de digitação corrompe o estoque.
