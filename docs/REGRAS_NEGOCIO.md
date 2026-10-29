# Regras de Negócio - Sistema Loja

## 1) Vendas / PDV
- **Caixa obrigatório**: Só é possível registrar vendas com um caixa aberto. A validação é feita no `VendaService` antes de persistir.
- **Itens obrigatórios**: Deve haver pelo menos um item na venda.
- **Formas de pagamento**: Obrigatórias. O valor pago deve ser >= total da venda (permite troco no fluxo).
- **Cartão (crédito/débito)**: Ao pagar com cartão, a venda passa pela operadora via `OperadoraCartaoFactory`. Apenas com resposta aprovada a venda é registrada.
- **Promoções**: Aplicadas conforme vigência por dia/horário e regras cadastradas. Em caso de múltiplas promoções aplicáveis, prioriza-se o menor preço/resultante mais vantajoso conforme `PromocaoService`.
- **Autorização**: Descontos acima do permitido ou ações sensíveis podem exigir autorização via `AutorizacaoService`.
- **Fidelidade**: Pode-se acumular/resgatar pontos conforme `FidelidadeService`, quando aplicável.

## 2) Caixa
- **Abertura**: Não pode haver outro caixa aberto.
- **Movimentações**: Sangria/suprimento exigem caixa aberto e autorização quando necessário.
- **Fechamento**: Calcula saldo esperado (fundo inicial + suprimentos + vendas por forma - sangrias). Exige conferência do valor informado.
- **Conciliação TEF**: Suporte a conciliação via interface abstrata (simulada). Destina-se a cruzar transações com extrato da adquirente.

## 3) Estoque / Compras
- **Entrada de mercadoria**: Registra `EntradaMercadoria` com itens, atualiza estoque e recalcula **custo médio ponderado**.
- **Custo médio ponderado**: `(estoqueAtual * custoMedioAtual + quantidadeEntrada * valorUnitarioEntrada) / (estoqueAtual + quantidadeEntrada)`. Arredondamento conforme regras da aplicação.
- **Fornecedor**: Cadastro e vínculo com entradas.
- **Sugestão de compra**: Baseada em consumo médio no período + `estoque_minimo` de cada produto.

## 4) Promoções
- **Vigência**: Por data e por horário (dia da semana/intervalo). Só ativas no período.
- **Aplicação**: Pode ser por produto. No carrinho é reprecificado respeitando vigência.
- **Menor preço**: Quando houver conflito, aplica-se o menor preço efetivo conforme lógica do serviço.

## 5) Devoluções
- **Validação**: Exige venda original válida e controle de **unidades disponíveis** para devolução.
- **Impacto**: Afeta estoque e integridade da venda original. Regras impedem devolução acima do permitido.

## 6) Fidelidade / Pontos
- **Acúmulo**: Conforme regras de configuração.
- **Resgate**: Aplicado no PDV com validações.

## 7) Autorização / Supervisão
- **Perfis**: `ADMIN` e `OPERADOR`. Ações sensíveis exigem usuário autorizado conforme `AutorizacaoService`.

## 8) TEF / Taxas
- **Abstração**: `OperadoraCartao` permite plugar SDK real futuramente. Em dev/testes usa-se `OperadoraCartaoSimulada`.
- **Taxas por bandeira/tipo/parcela**: Configuradas em `taxas_cartao`. Cálculo: `valorTaxa = (valorBruto * taxaPercentual/100) + taxaFixa`, `valorLiquido = valorBruto - valorTaxa`.
- **Conciliação**: Apoiada no fechamento/relatórios para valores a receber por adquirente/bandeira.
