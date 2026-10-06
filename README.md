# Sistema de Gestão de Loja / PDV

[![CI](https://github.com/EduardoAlvez/sistema-loja/actions/workflows/ci.yml/badge.svg)](https://github.com/EduardoAlvez/sistema-loja/actions/workflows/ci.yml)
[![Licença MIT](https://img.shields.io/badge/licen%C3%A7a-MIT-blue.svg)](LICENSE)

Sistema completo de gestão de loja com **PDV (ponto de venda)** em **Java Desktop (Swing)** e **MySQL**,
desenvolvido como projeto de portfólio para demonstrar arquitetura em camadas, JDBC puro,
regras de negócio e testes automatizados.

---

## Funcionalidades

### Acesso
- **Login** com perfis distintos: **Administrador** e **Operador de caixa**
- Senhas armazenadas com **SHA-256 + salt por usuário** e comparação *constant-time*
- Operador não acessa o gerenciamento de usuários

### Dashboard
- Cards de **vendas do dia**, **faturamento**, **ticket médio** e **itens com estoque baixo**
- Tabelas de **produtos mais vendidos** e **estoque crítico** (≤ 5 un.)

### PDV (Ponto de Venda)
- Busca por **código de barras** (Enter adiciona direto) ou **nome**, com duplo clique
- **Carrinho editável**: quantidade alterável na própria tabela, itens duplicados são somados
- **4 formas de pagamento**: dinheiro, débito, crédito e PIX, com **cálculo de troco** em tempo real
- Seleção de cliente opcional e **cupom não fiscal** gerado no fim da venda
- Baixa de estoque **transacional** (commit/rollback) e validação de estoque antes de vender

### Cadastros
- **Produtos**: código de barras único, categoria (criada em linha de comando), preço de custo/venda,
  margem calculada, estoque e situação ativo/inativo
- **Clientes**: CPF com **validação real de dígitos verificadores**, e-mail, busca e exclusão
- **Usuários** (somente admin): criação com perfil e validação de senha

### Relatórios
- Período livre (de/até) com **vendas, faturamento e ticket médio**
- Histórico de vendas com status e **cancelamento de venda com devolução automática de estoque**
- **Mais vendidos** do período e lista de **estoque baixo**
- **Gráfico de faturamento por dia** (barras desenhadas com `Graphics2D`, sem biblioteca externa),
  com tooltip ao passar o mouse sobre cada barra
- **Aba Auditoria** (somente admin): trilha de ações do sistema no período

### Exportação
- **PDF** (OpenPDF): relatório com resumo, **gráfico de faturamento por dia**, tabela de vendas,
  mais vendidos, zebra de linhas, destaque para vendas canceladas e número de página no rodapé
- **Excel .xlsx** (Apache POI): 3 abas (`Resumo`, `Vendas`, `Mais vendidos`) com cabeçalho
  destacado, moeda formatada, largura de colunas e painéis congelados
- Nome de arquivo sugerido com o período (`relatorio_vendas_2026-10-01_a_2026-10-06.pdf`)
  e caixa de diálogo para escolher onde salvar

### Confiabilidade e governança
- **Logs** em `logs/sistema-loja.log` (rotação 5 MB × 3) e console via **Log4j2**:
  erros inesperados da interface guardam a stack trace completa, junto de ações como
  login, vendas, cancelamentos e exportações
- **Migrações de banco com Flyway** (`V1` esquema, `V2` auditoria), com histórico
  consultável e `baseline` automático para bancos já existentes
- **Trilha de auditoria** (tabela `auditoria`, append-only): login ok/falha, venda
  registrada/cancelada, produto/cliente salvos e usuário criado — com usuário, data/hora
  e detalhe; nunca grava senha

---

## Como executar

### Pré-requisitos
- **JDK 21** ou superior
- **Maven 3.9+**
- **Docker** (mais fácil) ou um **MySQL 8** local

### 1. Subir o banco de dados

```bash
docker compose up -d
```

> Já tem o container antigo (`docker run`)? Remova-o antes: `docker rm -f mysql-sistema-loja`.
> Os dados vivem no volume `mysql-data`; numa máquina nova o banco começa com o seed de exemplo.

O esquema é versionado com **Flyway** (`src/main/resources/db/migration`) e os dados de
exemplo entram na primeira execução. Bancos criados antes do Flyway recebem `baseline`
automático — nada de reaplicar DDL em cima de dados.

```bash
# histórico de migrações (opcional)
mvn flyway:info -Dflyway.url=jdbc:mysql://localhost:3306/sistema_loja \
  -Dflyway.user=root -Dflyway.password=root
```

### 2. Executar

```bash
mvn clean package
java -jar target/sistema-loja-1.0.0.jar
```

Ou pela IDE: execute a classe `com.portfolio.sistemaloja.App`.

### Credenciais padrão

| Perfil | Login | Senha |
|--------|-------|-------|
| Administrador | `admin` | `admin123` |
| Operador de caixa | `operador` | `caixa123` |

### Configuração por variáveis de ambiente (opcional)

| Variável | Padrão |
|----------|--------|
| `DB_URL` | `jdbc:mysql://localhost:3306/sistema_loja?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC` |
| `DB_USER` | `root` |
| `DB_PASS` | `root` |

### Testes

```bash
mvn test
```

**67 testes** cobrindo serviços, validações, senhas, o ciclo completo de venda,
a exportação em PDF/Excel, o gráfico, as migrações do Flyway e a trilha de auditoria —
rodam sobre **H2 em modo MySQL**, ou seja, não dependem do Docker.

---

## Arquitetura

Projeto em **quatro camadas**, com dependência sempre de cima para baixo:

```
┌──────────────────────────────────────────────────────┐
│ ui/          Swing: telas, diálogos e formulários    │
├──────────────────────────────────────────────────────┤
│ service/     Regras de negócio e validações          │
├──────────────────────────────────────────────────────┤
│ repository/  JDBC: SQL, mapeamento e transações      │
├──────────────────────────────────────────────────────┤
│ model/       Entidades e enums                       │
│ db/          Conexão, migração e hash de senhas      │
└──────────────────────────────────────────────────────┘
```

| Camada | Pacote | Responsabilidade |
|--------|--------|------------------|
| UI | `ui` | `TelaLogin`, `TelaPrincipal` com abas, painéis e diálogos |
| Serviço | `service` | `VendaService`, `ProdutoService`, `ClienteService`, `AuthService`, `RelatorioService`, `AuditoriaService`, `Cpf` |
| Repositório | `repository` | `ProdutoRepository`, `VendaRepository`, `ClienteRepository`, `UsuarioRepository`, `CategoriaRepository`, `RelatorioRepository`, `AuditoriaRepository` |
| Modelo | `model` | `Produto`, `Cliente`, `Venda`, `VendaItem`, `Usuario`, enums |
| Infra | `db` | `Banco` (fonte de conexão), `Migrador` (Flyway + seed), `Senhas` |

A interface recebe uma única instância de `Aplicacao`, que monta toda a árvore de dependências —
a mesma classe é usada pelos testes apontando para o H2.

---

## Estrutura do projeto

```
sistema-loja/
├── pom.xml                                  # Build Maven (Java 21, shade + exec)
├── src/
│   ├── main/
│   │   ├── java/com/portfolio/sistemaloja/
│   │   │   ├── App.java                     # Ponto de entrada: FlatLaf + migração + login
│   │   │   ├── Aplicacao.java               # Composição das dependências (DI manual)
│   │   │   ├── Sessao.java                  # Usuário logado (contexto da auditoria)
│   │   │   ├── db/                          # Banco, Migrador (Flyway), Senhas, ConexaoFonte
│   │   │   ├── grafico/                     # GraficoFaturamento: render com Graphics2D
│   │   │   ├── model/                       # Produto, Cliente, Venda, Usuario, enums
│   │   │   ├── repository/                  # 7 repositórios JDBC + records de relatório
│   │   │   ├── service/                     # Regras de negócio e validações (10 classes)
│   │   │   └── ui/                          # 12 classes Swing (telas, painéis, diálogos)
│   │   └── resources/
│   │       ├── db/migration/                # V1__esquema_inicial.sql + V2__auditoria.sql
│   │       └── log4j2.xml                   # Logs: console + arquivo com rotação
│   └── test/java/com/portfolio/sistemaloja/ # 67 testes JUnit 5
└── target/                                  # Build (ignorada no git)
```

---

## Tecnologias

- **Java 21** + **Swing** (interface desktop)
- **FlatLaf 3.5** (visual moderno nativo do Swing)
- **Gráficos com `Graphics2D`** da própria JDK (nenhuma dependência de biblioteca de charts)
- **MySQL 8** via **mysql-connector-j** (JDBC puro, sem ORM)
- **Flyway** (migrações versionadas do esquema, com baseline para bancos existentes)
- **Log4j2** (logs em arquivo com rotação e no console)
- **H2** no modo MySQL (banco dos testes)
- **Apache POI 5.5** (exportação em `.xlsx`) + **OpenPDF 3** (exportação em PDF)
- **JUnit 5** + Maven Surefire
- **Maven Shade** (jar executável único)

---

## Notas de implementação

- **Venda em transação**: `VendaRepository.registrar()` faz `commit` da venda + itens + baixa de
  estoque juntos; qualquer falha (ex.: estoque insuficiente) dispara `rollback` e nada é gravado.
- **Estoque protegido no SQL**: `UPDATE produtos SET estoque = estoque + ? WHERE ... AND estoque + ? >= 0`,
  ou seja, a proteção contra corrida entre caixas está no próprio banco.
- **Cancelamento devolve estoque**: mesma transação, e a venda fica registrada com status `CANCELADA`
  para manter a trilha de auditoria (nunca é apagada).
- **Migrações versionadas**: esquema no Flyway (`V1` esquema inicial, `V2` auditoria);
  bancos já existentes recebem `baselineOnMigrate`, então o histórico nasce sem reaplicar DDL.
  O seed continua idempotente (só roda se a tabela `usuarios` estiver vazia).
- **Auditoria fora da transação**: os services gravam na trilha com `try/catch` próprio —
  se a gravação falhar, o erro vai para o log e a operação principal não é afetada.
- **Erros da interface com stack trace**: `Ui.erro(..., causa)` loga a exceção completa antes
  de mostrar a mensagem amigável; validações previstas ficam só no diálogo, sem poluir o log.
- **Números de venda sequenciais** (`V000001`, `V000002`...) com constraint de unicidade.
- **CPF de verdade**: algoritmo dos dois dígitos verificadores, rejeita sequências repetidas
  e guarda o CPF formatado (`000.000.000-00`).
- **Sem SQL dinâmico**: todas as consultas usam `PreparedStatement`, inclusive os filtros de busca.

---

## Roadmap

- [x] Migração de esquema + dados de exemplo
- [x] Login com perfis e senha com hash
- [x] CRUD de produtos, clientes e categorias
- [x] PDV com carrinho, pagamento e cupom
- [x] Cancelamento de venda com devolução de estoque
- [x] Dashboard e relatórios por período
- [x] Gráfico de faturamento por dia (Graphics2D) na tela e no PDF
- [x] Exportação de relatórios em PDF e Excel (.xlsx)
- [x] Migrações de banco com Flyway (V1 esquema, V2 auditoria, baseline)
- [x] Logs em arquivo com rotação e stack traces da interface (Log4j2)
- [x] Trilha de auditoria com consulta na aba Relatórios (admin)
- [x] 67 testes JUnit (serviços, UI, migrações e integração sobre H2)
- [x] Jar executável com `mvn package`
- [ ] Gráfico de vendas por dia no dashboard
- [ ] Importação/exportação de produtos em CSV
- [ ] Tecla de atalho global para o PDV
- [ ] Publicar no GitHub

---

## Autor

**Eduardo Alvez** — [byteswood@gmail.com](mailto:byteswood@gmail.com)

Projeto de portfólio — Sistema de Gestão de Loja / PDV em Java Desktop.
