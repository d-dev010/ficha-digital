# Adicionar Endereço do Cliente e Pessoa que Retirou na Compra

Este plano descreve as alterações necessárias no backend e frontend para suportar a adição do endereço do cliente e o nome da pessoa que retirou o item na hora da compra. O horário da compra já é registrado nativamente pelo campo `data` (Instant).

## User Review Required

> [!IMPORTANT]
> **1. O campo "Pessoa que pegou o item" deve ser obrigatório?**  
> Na minha proposta atual, ele será *opcional*. Se for deixado em branco, assumiremos que o próprio cliente retirou.
> 
> **2. Sobre o Dia e Hora da retirada:**  
> O sistema já salva automaticamente a data e a hora exatas no momento em que você registra a compra no sistema. **Isso é suficiente, ou você precisa de uma opção para alterar manualmente a data e hora (por exemplo, se você esqueceu de anotar na hora e quer registrar depois)?**
> 
> *Nota: Independentemente da sua escolha acima, vou garantir que a data e a hora fiquem bem visíveis na listagem de compras do cliente.*

## Proposed Changes

### Banco de Dados (Migrations)
- Criar nova migration `V8__add_endereco_e_pessoa_retirou.sql`:
  - `ALTER TABLE cliente ADD COLUMN endereco VARCHAR(255);`
  - `ALTER TABLE lancamento ADD COLUMN pessoa_retirou VARCHAR(255);`

***

### Backend (Spring Boot)
Atualização das entidades e objetos de transferência (DTOs):

#### [MODIFY] `Cliente.java`
- Adicionar atributo `String endereco`.

#### [MODIFY] `Lancamento.java`
- Adicionar atributo `String pessoaRetirou`.

#### [MODIFY] DTOs de Cliente
- Adicionar `endereco` nos seguintes DTOs:
  - `CadastrarClienteRequest`
  - `ClienteDetalhe`
  - `ClienteResumo`

#### [MODIFY] DTOs de Lançamento
- Adicionar `pessoaRetirou` nos seguintes DTOs:
  - `LancarFiadoRequest`
  - `LancamentoResponse`

#### [MODIFY] Services
- Atualizar a lógica do `ClienteService` e `LancamentoService` para mapear e persistir esses novos campos caso eles sejam enviados.

***

### Frontend (Angular)
Atualização da interface do usuário para coletar e exibir os novos campos.

#### [MODIFY] Modelos (`cliente.model.ts` e `lancamento.model.ts`)
- Adicionar a propriedade `endereco?: string` no cliente.
- Adicionar a propriedade `pessoaRetirou?: string` no lançamento.

#### [MODIFY] `novo-cliente-dialog.component`
- Incluir o campo de texto "Endereço" no formulário de criação de cliente.

#### [MODIFY] `lancar-fiado-dialog.component`
- Incluir o campo de texto "Quem pegou o item?" no formulário de lançar compra (fiado).

#### [MODIFY] `cliente-extrato.component`
- Adicionar a exibição do endereço no cabeçalho/detalhes do cliente.
- Na tabela de histórico, adicionar uma coluna "Retirado por" (ou exibir junto à descrição) e garantir que a formatação da "Data/Hora" exiba também a hora do lançamento (ex: `dd/MM/yyyy HH:mm`).

## Verification Plan

### Testes Manuais
- Iniciar o backend e frontend.
- Criar um novo cliente preenchendo o campo Endereço e verificar se ele é salvo e exibido no perfil/extrato do cliente.
- Realizar um novo lançamento (Compra) para esse cliente, preenchendo o nome de uma pessoa terceira que pegou o item.
- Verificar no extrato do cliente se a compra aparece com a data/hora correta e a indicação de quem retirou.
