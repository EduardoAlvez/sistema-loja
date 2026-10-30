# Principais Validações (ValidacaoException)

## Vendas / PDV
- **Caixa obrigatório**: "Abra o caixa antes de registrar uma venda."
- **Itens obrigatórios**: "Adicione pelo menos um item à venda."
- **Formas de pagamento**: "Selecione a forma de pagamento." / "Informe as formas de pagamento."
- **Valor pago**: "O valor pago é inferior ao total da venda."
- **Cartão TEF**: Transação negada pela operadora gera `ValidacaoException` com mensagem da resposta.
- **Descontos/Autorização**: Conforme regras do `AutorizacaoService`.

## Caixa
- **Já existe caixa aberto**: "Já existe um caixa aberto. Feche-o antes de abrir outro."
- **Sem caixa aberto**: "Não há caixa aberto para fechar."
- **Valor inválido**: "O valor informado de fechamento não pode ser negativo."
- **Operador inválido**: "É necessário estar autenticado para abrir o caixa."

## Estoque / Compras
- **Entrada inválida**: Dados obrigatórios faltando ou valores inválidos.
- **Fornecedor**: Regras de validação em `FornecedorService`.

## Cartão / TEF
- **Dados inválidos**: Transação sem valor ou dados obrigatórios.
- **Taxas**: Configuração inválida em `TaxaCartaoService`.
