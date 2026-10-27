# Lições aprendidas - Sistema Loja

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

## Lições aprendidas

- **Compilar frequentemente**: Em features grandes, compilar a cada bloco evita acumular centenas de erros.
- **Seguir padrões existentes**: Olhar arquivos similares antes de criar novos (helpers `Ui.*`, estrutura de tabelas/modelos).
- **Cuidado com transações**: Em operações multi-tabela (entrada + estoque + custo médio), usar transação atômica manual quando necessário.
- **Testes espelham UI**: Adicionar abas/itens na UI quebra testes com contagem fixa. Sempre revisar testes relacionados.
- **Commits pequenos**: Facilita debugging e mantém histórico limpo.
- **Verificar enum/constantes**: Sempre conferir valores reais (`PerfilUsuario.ADMIN` vs `ADMINISTRADOR`).
- **APIs existentes primeiro**: Antes de criar helpers novos, verificar se já existem na classe (`Ui`, `Aplicacao`, etc.).
