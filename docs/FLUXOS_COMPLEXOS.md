# Fluxos Complexos com Exemplos Numéricos

## 1) Custo médio ponderado (Entrada de mercadoria)
**Fórmula:** `custoMedioNovo = (estoqueAtual * custoMedioAtual + qtdEntrada * valorUnitario) / (estoqueAtual + qtdEntrada)`

**Exemplo:**
- Estoque atual: 10 unidades, custo médio: R$ 5,00 -> valor em estoque: R$ 50,00
- Entrada: 5 unidades a R$ 7,00 -> +R$ 35,00
- Total: 15 unidades, valor: R$ 85,00
- **Novo custo médio:** R$ 85,00 / 15 = **R$ 5,666...**

A entrada é realizada em transação (`EntradaMercadoriaRepository`) para garantir atomicidade entre entrada, itens e atualização de estoque/custo.

## 2) Fechamento de caixa
**Componentes:**
- Fundo inicial
- Suprimentos
- Vendas por forma de pagamento
- Sangrias
- **Saldo esperado** = (fundo inicial + suprimentos + vendas) - sangrias

**Exemplo numérico:**
| Componente | Valor |
|------------|-------|
| Fundo inicial | R$ 100,00 |
| Suprimentos | R$ 0,00 |
| Vendas (Dinheiro) | R$ 250,00 |
| Vendas (Cartão) | R$ 150,00 |
| Sangrias | R$ 30,00 |
| **Saldo esperado** | **R$ 470,00** |

No fechamento, compara-se com valor conferido informado pelo operador. Diferença é registrada.

## 3) Promoções por vigência
- Validadas por **data inicial/final** e **horário/dia da semana**.
- Aplicadas no PDV; reprecificação considera vigência no momento.
- Em conflito entre promoções aplicáveis, prioriza-se o **menor preço efetivo**.

## 4) Devolução afetando estoque
- Verifica **unidades disponíveis** para devolução com base na venda original.
- Não permite devolução acima do permitido.
- Impacta estoque da venda devolvida, mantendo integridade entre venda e devolução.

## 5) TEF por interface abstrata
**Fluxo:**
1. PDV seleciona cartão (crédito/débito) e parcelas
2. `VendaService` monta `TransacaoCartao` e chama `OperadoraCartaoFactory.getOperadora().autorizar(...)`
3. `OperadoraCartaoSimulada` retorna `RespostaTransacao` (aprovada/negada com NSU/código)
4. Se negada -> `ValidacaoException`; se aprovada -> venda registrada
5. Taxas: `TaxaCartaoService.calcular()` aplica `(valorBruto*%/100)+fixa` para relatório/valores a receber
6. Conciliação no fechamento via `CaixaService.conciliarCartoes()`
