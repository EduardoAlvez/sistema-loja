# Sistema de Gestão de Loja / PDV 🏪

Sistema completo de gestão de loja com **PDV (ponto de venda)** em **Java Desktop (Swing)** e **MySQL**,
desenvolvido como projeto de portfólio para demonstrar arquitetura em camadas, JDBC puro,
regras de negócio e testes automatizados.

---

## ✨ Funcionalidades

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

---

## 🚀 Como executar

### Pré-requisitos
- **JDK 21** ou superior
- **Maven 3.9+**
- **Docker** (mais fácil) ou um **MySQL 8** local

### 1. Subir o banco de dados

```bash
docker run -d --name mysql-sistema-loja -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=sistema_loja \
  --restart unless-stopped mysql:8.0
```

O esquema e os dados de exemplo são criados **automaticamente** na primeira execução.

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

**39 testes** cobrindo serviços, validações, senhas e o ciclo completo de venda —
rodam sobre **H2 em modo MySQL**, ou seja, não dependem do Docker.

---

## 🏗️ Arquitetura

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
| Serviço | `service` | `VendaService`, `ProdutoService`, `ClienteService`, `AuthService`, `RelatorioService`, `Cpf` |
| Repositório | `repository` | `ProdutoRepository`, `VendaRepository`, `ClienteRepository`, `UsuarioRepository`, `CategoriaRepository`, `RelatorioRepository` |
| Modelo | `model` | `Produto`, `Cliente`, `Venda`, `VendaItem`, `Usuario`, enums |
| Infra | `db` | `Banco` (fonte de conexão), `Migrador` (DDL + seed), `Senhas` |

A interface recebe uma única instância de `Aplicacao`, que monta toda a árvore de dependências —
a mesma classe é usada pelos testes apontando para o H2.

---

## 📁 Estrutura do projeto

```
sistema-loja/
├── pom.xml                                  # Build Maven (Java 21, shade + exec)
├── src/
│   ├── main/
│   │   ├── java/com/portfolio/sistemaloja/
│   │   │   ├── App.java                     # Ponto de entrada: FlatLaf + migração + login
│   │   │   ├── Aplicacao.java               # Composição das dependências (DI manual)
│   │   │   ├── db/                          # Banco, Migrador, Senhas, ConexaoFonte
│   │   │   ├── model/                       # Produto, Cliente, Venda, Usuario, enums
│   │   │   ├── repository/                  # 6 repositórios JDBC + records de relatório
│   │   │   ├── service/                     # Regras de negócio e validações (8 classes)
│   │   │   └── ui/                          # 11 classes Swing (telas, painéis, diálogos)
│   │   └── resources/sql/esquema.sql        # DDL idempotente (CREATE TABLE IF NOT EXISTS)
│   └── test/java/com/portfolio/sistemaloja/ # 39 testes JUnit 5
└── target/                                  # Build (ignorada no git)
```

---

## 🛠️ Tecnologias

- **Java 21** + **Swing** (interface desktop)
- **FlatLaf 3.5** (visual moderno nativo do Swing)
- **MySQL 8** via **mysql-connector-j** (JDBC puro, sem ORM)
- **H2** no modo MySQL (banco dos testes)
- **JUnit 5** + Maven Surefire
- **Maven Shade** (jar executável único)

---

## 📝 Notas de implementação

- **Venda em transação**: `VendaRepository.registrar()` faz `commit` da venda + itens + baixa de
  estoque juntos; qualquer falha (ex.: estoque insuficiente) dispara `rollback` e nada é gravado.
- **Estoque protegido no SQL**: `UPDATE produtos SET estoque = estoque + ? WHERE ... AND estoque + ? >= 0`,
  ou seja, a proteção contra corrida entre caixas está no próprio banco.
- **Cancelamento devolve estoque**: mesma transação, e a venda fica registrada com status `CANCELADA`
  para manter a trilha de auditoria (nunca é apagada).
- **Migração idempotente**: o `esquema.sql` usa `IF NOT EXISTS` e o seed só roda se a tabela
  `usuarios` estiver vazia — dá para executar o sistema quantas vezes quiser.
- **Números de venda sequenciais** (`V000001`, `V000002`...) com constraint de unicidade.
- **CPF de verdade**: algoritmo dos dois dígitos verificadores, rejeita sequências repetidas
  e guarda o CPF formatado (`000.000.000-00`).
- **Sem SQL dinâmico**: todas as consultas usam `PreparedStatement`, inclusive os filtros de busca.

---

## 📌 Roadmap

- [x] Migração de esquema + dados de exemplo
- [x] Login com perfis e senha com hash
- [x] CRUD de produtos, clientes e categorias
- [x] PDV com carrinho, pagamento e cupom
- [x] Cancelamento de venda com devolução de estoque
- [x] Dashboard e relatórios por período
- [x] 39 testes JUnit (serviços + integração sobre H2)
- [x] Jar executável com `mvn package`
- [ ] Gráfico de vendas por dia no dashboard
- [ ] Importação/exportação de produtos em CSV
- [ ] Tecla de atalho global para o PDV
- [ ] Publicar no GitHub

---

## 👤 Autor

**Eduardo Alvez** — [byteswood@gmail.com](mailto:byteswood@gmail.com)

Projeto de portfólio — Sistema de Gestão de Loja / PDV em Java Desktop.
