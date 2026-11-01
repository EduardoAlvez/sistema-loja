# Lições aprendidas - Sistema Loja

## Lições aprendidas - Sistema Loja

## Sessão: Revisão de código do Inventário - issue #41 (31/10/2026)

### Revisão por agentes: achados aplicados
| Achado | Causa | Correção |
|--------|-------|----------|
| Auditoria gravada em **conexão separada, antes do commit** | `AuditoriaRepository` abre a própria conexão; o `INSERT` da auditoria não participava da transação. Commit falhando deixava evento órfão; auditoria falhando deixava perda sem rastro. | Overloads transacionais `AuditoriaRepository.registrar(..., Connection)` e `AuditoriaService.registrarComo(..., Connection)`. A auditoria passa a ser confirmada/revertida junto com a baixa de estoque (mesmo padrão de `ConfiguracaoService`). |
| `observacao` sem limite de tamanho | `VARCHAR(255)` sem validação -> erro cru do driver (`SQLState 22001`) exibido ao usuário final. | `InventarioService.OBSERVACAO_MAX` validado no serviço + `PlainDocument` limitando a digitação na UI. |
| `rollback()`/`setAutoCommit(true)` mascaravam a exceção original | O `finally` lançava `SQLException` e substituía o erro de negócio em voo. | `rollbackSilencioso` com `addSuppressed` (padrão de `ConfiguracaoService`) e remoção do `finally` redundante. |
| `ValidacaoException` sem causa | Construtor único `(String)`; a exceção original era perdida. | Novo construtor `(String, Throwable)` - aditivo, não quebra chamadores. |
| Autorização dependia de `Usuario` fornecido pelo chamador | `Usuario` é POJO mutável: um chamador poderia forjar um admin. | Autorização passa a vir da `Sessao` (padrão de `ConfiguracaoService`/`AutorizacaoService`), com checagem de `ativo`. |
| Lista de motivos duplicada entre UI e serviço | Duas listas que podiam divergir. | `enum MotivoPerda` como fonte única; `OUTROS` passou a **exigir observação** (regra que só existia no papel). |
| Campos do formulário sem nome acessível | `new JLabel(...)` sem `setLabelFor` -> leitor de tela anuncia "campo de texto" sem dizer qual (WCAG 4.1.2 Nível A). | Novo `Ui.rotulo(texto, campo)` (sobrecarga, não quebra os existentes) + `PainelInventario` usando-o. |
| Baixa de estoque sem confirmação | Ação irreversível a um clique, com o primeiro produto já selecionado. | `Ui.confirma` antes de registrar. |
| `RuntimeException` escapando na EDT | `PerdaEstoqueRepository` lança `RepositorioException`; a falha podia impedir a janela principal de abrir. | `catch (RuntimeException)` com `Ui.erro(pai, msg, causa)` em todos os caminhos de carga. |
| Tabela fora do design system | `new JTable(modelo)` -> linhas de 16px, sem zebra, sem ordenação. | `Ui.modeloTabela(...)` + `Ui.tabela(...)` (26px, zebra, `TableRowSorter`). |
| Histórico sem responsável | `usuario_id` existia no banco mas não era exibido - lacuna real de rastreabilidade. | `JOIN usuarios` no repositório + coluna "Responsável". |
| Schema morto | `V24__inventario_contagem.sql` criava duas tabelas sem nenhum código usando (Fase 2 não iniciada), travando o schema futuro. | Tabelas de contagem removidas; perdas consolidadas em `V24__perdas_estoque.sql`. A Fase 2 cria `V25__inventario_contagem.sql` com o schema definitivo. |

### Lições aprendidas
- **Auditoria em operação transacional deve usar a MESMA conexão**: ou grava na transação, ou grava depois do commit. Nunca em conexão paralela antes do commit.
- **Validar tamanho de campo na fronteira**: `VARCHAR(n)` no banco não protege a UX. O limite deve existir no serviço (fonte da verdade) e na UI (evitar digitação inútil).
- **Rollback nunca deve mascarar a causa**: usar `rollbackSilencioso` + `addSuppressed`.
- **Autorização sensível vem da sessão**, não de parâmetro: torna o estado inválido (usuário forjado) não representável.
- **Enum para domínios fechados**: uma lista duplicada entre UI e serviço é divergência esperando acontecer.
- **Rótulo acessível é `setLabelFor`**: `new JLabel("X")` sozinho não nomeia o campo para leitor de tela.
- **Ação irreversível pede confirmação**: perda de estoque não tem estorno na UI.
- **`RuntimeException` na EDT vira tela quebrada**: sempre capturar nos painéis, inclusive nos construtores.
- **Tabelas do projeto têm design system**: `Ui.modeloTabela` + `Ui.tabela`, nunca `new JTable`.
- **Tabelas de histórico devem mostrar o responsável**: é informação de rastreabilidade, não decoração.

### Ferramenta: verificar o disco, não apenas a saída da edição
| Sintoma | Causa | Solução |
|---------|-------|---------|
| `read`/`edit` mostravam uma linha diferente do conteúdo real em disco (`preparado` vs `preparada`; `"caxa123"` vs `"caixa123"`) | A saída da ferramenta de edição não refletiu com fidelidade os bytes gravados. | **Confirmar sempre com `grep`/`sed`/`awk`/compilador antes de confiar.** O compilador e o teste são a autoridade final. |
| Edit "greedy" removeu linhas adjacentes | O `oldString` casou em mais de um ponto ou engoliu contexto. | Editar com contexto único e **reler o arquivo com `awk` numerado** logo depois. |
| Erro de compilação "`cannot find symbol`" em arquivo recém-editado | O `edit` reportou sucesso, mas o conteúdo em disco estava corrompido. | `mvn -o compile` imediatamente após cada edição, nunca em lote. |

## Sessão: Issue #41 - Inventário (31/10/2026)

### Diagnóstico das falhas de CI após múltiplos commits

| Erro | Causa | Solução |
|------|-------|---------|
| CI falhou no `compile`: `cannot find symbol` em `Aplicacao.java` e `InventarioService.java` | Push de commits intermediários com wiring apontando para classes já removidas (`ContagemInventarioRepository`) e import com typo (`PerdidaEstoqueRepository` em vez de `PerdaEstoqueRepository`). | Corrigir imports e wiring, e **não fazer push de commits intermediários que não compilam**. |
| 272 testes falhando com `Flyway Found more than one migration with version 24` | Duas migrações com a mesma versão: `V24__inventario_contagem.sql` e `V24__perdas_estoque.sql`. | Manter **uma única migração por versão**. Antes de criar `V<n>__x.sql`, conferir `ls src/main/resources/db/migration/`. |
| Mesma falha de Flyway persistindo após deletar o arquivo duplicado | `target/classes/db/migration/` mantinha o `.sql` deletado (Maven copia recursos, mas **não remove** os obsoletos). | Rodar `mvn -o clean test` sempre que deletar/renomear recurso, antes de concluir que a correção não funcionou. |
| Tabela `perda_estoque` criada duas vezes (V24 e V25) | A mesma tabela foi adicionada em duas migrações. | Separar responsabilidades: uma tabela por migração. `V24` = contagem, `V25` = perdas. |
| FK para tabela inexistente (`produto`/`usuario` vs `produtos`/`usuarios`) | Tabelas reais são no plural (`V1__esquema_inicial.sql`). | Conferir o schema real em `V1__esquema_inicial.sql` antes de escrever FK. |
| `usuario_id NOT NULL` no banco mas código passava `null` | Schema e código divergiram. | Validar `usuario.getId() != null` no service e sempre gravar `usuario_id`. |

#### Lições aprendidas
- **Push só com código verde**: `mvn -o clean test` local antes de cada push. Commits intermediários quebrados viram falhas vermelhas no GitHub e poluem o histórico.
- **Uma migração por versão**: O Flyway aborta com "more than one migration with version N". Verificar a pasta de migrações antes de criar uma nova.
- **Uma tabela por migração**: Não repetir `CREATE TABLE` em migrações diferentes, mesmo com schemas parecidos.
- **Maven não limpa recursos deletados**: `mvn clean` é obrigatório ao remover/renomear arquivos de `src/main/resources`.
- **Nome de tabela é plural**: `produtos`, `usuarios`, `clientes`, `vendas`. Sempre conferir em `V1__esquema_inicial.sql`.
- **Revisar o diff completo antes de commitar**: `git status` + `git diff` revelam wiring quebrado e imports faltando que a compilação local não pegou (por causa de `target/` stale).
- **Erros de digitação em imports**: `Perdida` vs `Perda`. Compilar logo após criar o arquivo pega isso na hora.

## Sessão: Issues #13, #14, #7 e #10 (27/10/2026)

### #10 – TEF por interface abstrata, taxa por bandeira e conciliação

#### Desafios encontrados
| Desafio | Causa | Solução |
|---------|-------|---------|
| Integração com fluxo existente de venda | `VendaService` já possuía fluxo consolidado; precisava autorizar cartão sem quebrar dinheiro/fiado. | Integrar chamada via `OperadoraCartaoFactory` apenas para `CARTAO_CREDITO`/`CARTAO_DEBITO`, aditiva e isolada em `tef/`. |
| Manter arquitetura limpa | Risco de acoplar PDV/serviços ao SDK da maquininha. | Criar pacote `com.portfolio.sistemaloja.tef` com contrato, DTOs, Factory e `simulado/`. Apenas interface + simulado nesta issue. |
| Concilição de cartões no fechamento | Necessário distinguir TEF por adquirente/bandeira sem alterar vendas históricas. | Adicionar ponto de conciliação (`CaixaService.conciliarCartoes`) e aba "Taxas TEF" em `PainelRelatorios`. |
| Configuração de taxas flexível | Taxas variam por bandeira, tipo e faixa de parcelas. | Criar `taxas_cartao` (V23) com `bandeira`, `tipo_cartao`, `parcelas_min/max`, `taxa_percentual`, `taxa_fixa`, `adquirente`. Serviço calcula `(valorBruto*%/100)+fixa`. |
| Testes com contagem de abas | Adicionar aba no `PainelRelatorios` quebrou 3 testes. | Ajustar expectativas para 9 (admin) / 8 (operador) e apontar `Auditoria` para a última aba. |

#### Erros e correções
| Erro | Causa | Solução |
|------|-------|---------|
| Compilação: `VendaRepository` não encontrado em `VendaService`/`Aplicacao` | Imports ausentes após refatoração. | Adicionar `import com.portfolio.sistemaloja.repository.VendaRepository;`. |
| Compilação: `VendaService` não encontrado em `Aplicacao` | Import ausente. | Adicionar `import com.portfolio.sistemaloja.service.VendaService;`. |
| `PainelRelatorios` sem imports | Uso de `JTextArea` sem import. | Adicionar `import javax.swing.JTextArea;`. |
| Testes UI quebrando | Ordem/quantidade de abas alterada. | Atualizar `PainelRelatoriosTest` para considerar "Taxas TEF" e posição de "Auditoria". |

#### Lições aprendidas
- **Abstração primeiro**: Interface + Factory permitem plugar SDK real futuramente sem tocar em PDV/VendaService.
- **Isolamento reduz risco**: Todo código TEF em `tef/` mantém acoplamento zero com integrações concretas.
- **Aditivo, não invasivo**: Autorizar apenas cartões novos; não alterar vendas já concluídas.
- **Testabilidade**: `OperadoraCartaoSimulada` com `forcarNegacao` facilita cenários sem maquininha.
- **Migrações compatíveis**: V23 simples, roda em MySQL/H2.
- **Pequenos commits**: Seguir plano por camada facilita revisão e rollback.
- **Um arquivo por vez**: Criar 1 arquivo, compilar, corrigir. Só depois ir pro próximo.
- **Checar APIs antes de usar**: Ler repositório/service antes de chamar. Não assumir `listarTodos()`.
- **Não contornar com reflection**: Corrigir imports/assinaturas corretamente.
- **Manter wiring consistente**: Manter `Aplicacao` sincronizado ao adicionar/remover serviços.
- **Reverter ao invés de lutar**: Se virar teia, reverter para `HEAD` e refazer commit por commit.

## Sessão: Issues #13, #14 e #7 (27/10/2026)

### #13 – Estoque crítico usando `estoque_minimo`
| Erro/Desafio | Causa | Solução |
|--------------|-------|---------|
| Teste falhando | `listaItensComEstoqueCritico` criava produto com `estoque(2)` mas `estoqueMinimo` padrão 0. Com regra `estoque <= estoque_minimo`, `2 <= 0` era falso. | Ajustar o teste para definir `produtoBaixo.setEstoqueMinimo(2)`. |

### #14 – Docker Compose para MySQL 8.4 LTS
| Erro/Desafio | Causa | Solução |
|--------------|-------|---------|
| Nenhum erro significativo | Apenas atualização de configuração | Trocar `mysql:8.0` por `mysql:8.4` e atualizar README. Validado com `docker compose config`. |

### #7 – Fornecedores, entrada de mercadoria, custo médio ponderado e sugestão de compra

#### Erros de compilação por API/UI
| Erro | Causa | Solução |
|------|-------|---------|
| `Ui.aviso()` não existe | Método não presente em `Ui.java` | Adicionar `aviso()` e `informacao()` mapeando para `JOptionPane.INFORMATION_MESSAGE`. |
| `Ui.janela()` não existe | Método não presente em `Ui.java` | Substituir por `SwingUtilities.getWindowAncestor(this)`. |
| `Ui.bordaVazia()` não existe | Método não presente em `Ui.java` | Substituir por `BorderFactory.createEmptyBorder(...)`. |
| `Ui.modelo()` vs `Ui.modeloTabela()` | Inconsistência com padrão do projeto | Usar `Ui.modeloTabela()` consistentemente (ou adicionar `Ui.modelo()` que delega). |
| `valorMonetario()` não existe | Método não presente em `Ui.java` | Usar `new BigDecimal(valorStr.trim().replace(",", "."))`. |

#### Erros por estrutura/métodos faltando
| Erro | Causa | Solução |
|------|-------|---------|
| `Aplicacao` sem getters | Novos services não expostos | Adicionar `getSessao()`, `getFornecedores()`, `getCompras()`, `getPromocoes()`. Usar `getUsuarioAtual()` quando aplicável. |
| Imports faltando | Esquecidos durante adições | Adicionar imports conforme erros (`FiadoRepository`, `DevolucaoRepository`, `ProdutoVendado`, `ArrayList`, etc.). |
| `PerfilUsuario.ADMINISTRADOR` não existe | Enum usa `ADMIN` e `OPERADOR` | Trocar para `PerfilUsuario.ADMIN`. |

#### Erros no repositório/serviço
| Erro | Causa | Solução |
|------|-------|---------|
| `FornecedorRepository.remover()` com `return false` após throw | Código inalcançável + chave extra | Remover return redundante e corrigir chaves. |
| `nomeExiste()` com return após catch | Código inalcançável | Remover return redundante. |
| `ProdutoRepository` sem métodos com Connection | `VendaRepository` e `EntradaMercadoriaRepository` dependiam deles | Adicionar `alterarEstoque(long, int, Connection)` e `buscarPorId(long, Connection)`. |
| `RelatorioRepository` sem `ArrayList` import | Uso sem import | Adicionar `import java.util.ArrayList;`. |

#### Erros na UI (PainelRelatorios)
| Erro | Causa | Solução |
|------|-------|---------|
| `modeloSugestao`/`tabelaSugestao` não encontrados | Declaração/local incorretos; `abas` local ao invés de campo | Tornar `abas` campo da classe, declarar modelos/tabelas como campos, adicionar aba "Sugestão de compra". |
| Teste `PainelRelatoriosTest` falhando | Contagem de abas alterada | Atualizar expectativa: admin 7→8, operador 6→7. |
| `inicio.getDate()` não existe | `JSpinner` usa `getValue()` | Converter com `toInstant().atZone(ZoneId.systemDefault()).toLocalDate()`. |

