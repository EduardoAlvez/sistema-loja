# Lições aprendidas - Sistema Loja

## Lições aprendidas - Sistema Loja

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

## Lições aprendidas - Sistema Loja

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
