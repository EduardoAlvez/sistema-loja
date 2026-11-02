# Plano Issue #41 - Inventário (contagem cíclica + perdas com motivo)

## Status
**Fase 1 (perdas com motivo) — CONCLUÍDA em 31/10/2026.**
**Fase 2 (contagem cíclica) — CONCLUÍDA em 02/11/2026**, com correções da revisão de código
aplicadas em 10 commits (races de abertura/fechamento, cancelamento, UI e acessibilidade),
validadas em MySQL 8.4 real e H2.

## Regras
- Opcional: não obrigatório para vender nem fechar caixa
- Visível apenas para GERENTE ou ADMIN
- Apenas aditivo (novas tabelas/migrações)
- Toda ação registrada em auditoria

## Schema (migrações)
- V24__perdas_estoque.sql
  - perda_estoque(id, produto_id, quantidade, motivo, usuario_id, observacao, data_hora)
- V25__inventario_contagem.sql (Fase 2, aplicada)
  - contagem_inventario(id, numero VARCHAR(24), status CHECK ABERTA/FECHADA/CANCELADA, usuario_id,
    observacao, motivo_cancelamento, data_hora, data_fechamento)
  - contagem_item(id, contagem_id, produto_id, estoque_sistema, quantidade_contada, delta_aplicado, ajustado)
  - contagem_controle(id) — linha sentinela com seed; serializa a abertura via SELECT ... FOR UPDATE

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

## Fase 2 - Contagem cíclica (concluída em 02/11/2026)

### Entregue
- [x] `model/ContagemInventario.java`, `model/ContagemItem.java` e `model/StatusContagem.java`
- [x] `repository/ContagemInventarioRepository.java`: abrir, informar, fechar **e cancelar**
- [x] `InventarioService`: `abrirContagem` (lote ou todos os ativos), `informarContagem`,
      `fecharContagem`, `cancelarContagem` — ajuste de estoque somente no fechamento,
      em transação única com leitura travada (`estoqueParaAjuste`)
- [x] `ui/DialogoNovaContagem.java`, `ui/DialogoContagemItem.java`, `ui/PainelContagem.java`
      como aba "Contagem cíclica" dentro de `PainelInventario`
- [x] Semântica absoluta validada em MySQL 8.4: a quantidade contada vira o estoque final
      mesmo com movimento durante a contagem (contada=40 com estoque travado 35 → final 40,
      `delta_aplicado` 5)
- [x] Cancelamento sem tocar no estoque, com motivo obrigatório em coluna própria

### Correções da revisão de código (races e robustez)
- [x] Duas aberturas simultâneas: serializadas pela sentinela `contagem_controle`
      (antes o "uma contagem por vez" só existia no código)
- [x] Fechamento concorrente: trava do cabeçalho **sem JOIN** + `UPDATE ... WHERE status='ABERTA'`
      conferindo `executeUpdate() == 1`; ajuste/delta/auditoria aplicados uma única vez
- [x] `informarContagem`: trava antes da leitura do status; `UPDATE` guardado por `EXISTS`
      de contagem ABERTA; id do item buscado direto (`buscarItemId`) em vez de materializar
      a contagem inteira a cada digitação
- [x] Ordem global de locks: sentinela → cabeçalho → produtos (por `produto_id`), sem ciclo
- [x] Rollback do fechamento testado: falha no 2º ajuste → estoques intactos, contagem segue
      ABERTA, `delta_aplicado` NULL, zero eventos de auditoria (hook `falharEmAjusteParaTeste`,
      padrão `DevolucaoRepository`)
- [x] Abertura rejeita produto inativo; autorização antes de `listarAtivos()`
- [x] `diferenca` renomeada para `delta_aplicado` (o homônimo do Caixa não foi tocado);
      `previaDeDiferenca()` devolve `Integer` (nulo ≠ zero)
- [x] Mapper falha alto (`RepositorioException`) em status desconhecido
- [x] Leitura do resultado final de fechar/cancelar fora da transação (sucesso não pode
      virar erro por falha de leitura)

### Correções de UI e acessibilidade
- [x] Título da aba "Perdas" estava com o slot NORTH disputado (`BorderLayout` substitui) — nunca
      era desenhado; título + formulário agora num único painel NORTH (medido: largura > 0)
- [x] `RepositorioException` escapando do `ActionListener` na EDT sem feedback (em
      `estoqueAtualDe`); erros técnicos com causa vão para `Ui.erro(..., causa)` que loga
- [x] Falha de carga limpa tabelas/estado e desabilita ações (sem modal: `Ui.erroDeCarga` loga;
      um diálogo a cada recarga de painel com o banco fora seria insuportável)
- [x] Filtro de `DialogoNovaContagem` preserva a marcação; seleção da tabela sobrevive ao
      recarregar
- [x] Enter registra e Esc fecha nos dois diálogos; foco inicial no campo de quantidade
- [x] "Iniciar contagem" desabilitado quando há contagem em aberto; "Cancelar contagem" com
      confirmação e motivo obrigatório
- [x] Nome acessível nas duas tabelas; coluna **Status** no histórico com "Cancelada" destacada
- [x] `AUTO_RESIZE_OFF` com larguras (código de barras deixava de ser legível); rótulo de
      status persistente após informar/cancelar

### Validação
- [x] 400 testes verdes no H2 (CI) — incl. `InventarioConcorrenciaTest`, vistos **falhando
      antes** da correção das races
- [x] MySQL 8.4.13 (container separado na porta 6033, sem tocar no banco de dev): V1–V25 do
      zero, fluxo completo, semântica absoluta, cancelamento e as duas races principais
- [x] Upgrade real: cópia do banco de dev (v20, com dados) migrada de V21–V25 sem perda
- [x] Notas: a validação anterior da issue #14 rodou em MySQL 8.0.46 (o `docker-compose.yml`
      declara 8.4, mas a stack nunca foi recriada) — comentário corrigido na issue
