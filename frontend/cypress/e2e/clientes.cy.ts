// =============================================================================
// SUITE: Gestão de Clientes
// OBJETIVO: Validar os fluxos da tela de listagem e busca de clientes.
//
// ESTRATÉGIA DE AUTENTICAÇÃO:
// Em vez de navegar pela UI de login antes de cada teste (o que é lento
// e já foi validado na suite login.cy.ts), usamos cy.request() para
// chamar a API diretamente e salvar o token no localStorage.
// Isso é uma prática profissional que torna os testes mais rápidos e
// focados — cada suite testa sua própria responsabilidade.
// =============================================================================

// Comando customizado para fazer login via API (não via UI)
// Isso evita repetir o fluxo de UI de login em toda suite que precisar de auth.
const loginViaApi = () => {
  cy.request({
    method: 'POST',
    url: 'http://localhost:8080/auth/login',
    body: {
      email: 'sla@gmail.com',
      senha: '@Killa003'
    }
  }).then((response) => {
    // Armazena o token no localStorage com a mesma chave que o AuthService usa ('fd_token')
    window.localStorage.setItem('fd_token', response.body.token)
  })
}

describe('Gestão de Clientes', () => {
  beforeEach(() => {
    // Faz login via API (rápido, sem UI) antes de cada teste
    loginViaApi()

    // Navega para a página de clientes
    cy.visit('/clientes')

    // Aguarda os dados carregarem (o spinner sumir e a lista aparecer)
    cy.get('.resultado-info', { timeout: 10000 }).should('be.visible')
  })

  // --------------------------------------------------------------------------
  // TC-06: Listagem de clientes
  // Por que testar isso? Garante que a integração frontend → backend → banco
  // está funcionando e que dados reais são exibidos na tela.
  // --------------------------------------------------------------------------
  it('TC-06 | Deve listar os clientes cadastrados com suas informações', () => {
    // Verifica o título da página
    cy.contains('h2', 'Clientes').should('be.visible')

    // Verifica que há pelo menos um card de cliente na lista
    cy.get('mat-card.cliente-card').should('have.length.greaterThan', 0)

    // Verifica que o contador de resultados é exibido
    cy.get('.resultado-info').should('contain', 'cliente(s)')

    // Verifica que cada card tem nome e saldo
    cy.get('mat-card.cliente-card').first().within(() => {
      cy.get('.cliente-nome').should('be.visible')
      cy.get('.saldo-valor').should('be.visible')
    })
  })

  // --------------------------------------------------------------------------
  // TC-07: Campo de busca por nome
  // Por que testar isso? A busca é a funcionalidade principal da tela.
  // Garante que o debounce e a integração com a API de busca funcionam.
  // --------------------------------------------------------------------------
  it('TC-07 | Deve filtrar clientes ao digitar no campo de busca', () => {
    // Captura o número inicial de clientes antes da busca
    cy.get('mat-card.cliente-card').its('length').as('totalAntes')

    // Digita no campo de busca
    cy.get('#input-busca').type('Ana')

    // Aguarda o debounce (geralmente 300ms) e a nova requisição
    // O resultado deve mudar após a busca
    cy.get('.resultado-info', { timeout: 5000 }).should('contain', 'cliente(s)')

    // Verifica que os cards exibidos contêm o texto buscado
    cy.get('mat-card.cliente-card').each(($card) => {
      cy.wrap($card).get('.cliente-nome').invoke('text').should('match', /Ana/i)
    })
  })

  // --------------------------------------------------------------------------
  // TC-08: Botão de limpar busca
  // Por que testar isso? Garante que o usuário consegue resetar o filtro
  // e ver a lista completa novamente, sem precisar limpar manualmente.
  // --------------------------------------------------------------------------
  it('TC-08 | Deve limpar a busca ao clicar no botão X', () => {
    // Realiza uma busca
    cy.get('#input-busca').type('Ana')
    cy.get('.resultado-info', { timeout: 5000 }).should('be.visible')

    // O botão de limpar (X) deve aparecer após digitar
    cy.get('mat-form-field button mat-icon').contains('close').click()

    // O campo deve estar vazio
    cy.get('#input-busca').should('have.value', '')

    // A lista completa deve reaparecer
    cy.get('.resultado-info', { timeout: 5000 }).should('be.visible')
  })

  // --------------------------------------------------------------------------
  // TC-09: Botão "Novo Cliente"
  // Por que testar isso? Garante que o ponto de entrada para cadastro
  // de clientes está acessível e funcional.
  // --------------------------------------------------------------------------
  it('TC-09 | Deve exibir o botão de novo cliente e estar clicável', () => {
    cy.get('#btn-novo-cliente').should('be.visible').and('not.be.disabled')
  })

  // --------------------------------------------------------------------------
  // TC-10: Navegação para extrato do cliente
  // Por que testar isso? Garante que o clique em um card navega corretamente
  // para a rota de extrato (/clientes/:id).
  // --------------------------------------------------------------------------
  it('TC-10 | Deve navegar para o extrato ao clicar em um cliente', () => {
    // Clica no primeiro card da lista
    cy.get('mat-card.cliente-card').first().click()

    // A URL deve mudar para /clientes/{algum-id}
    cy.url({ timeout: 5000 }).should('match', /\/clientes\/\d+/)
  })

  // --------------------------------------------------------------------------
  // TC-11: Logout
  // Por que testar isso? Garante que o token é removido e o usuário é
  // redirecionado para o login ao clicar em sair.
  // --------------------------------------------------------------------------
  it('TC-11 | Deve fazer logout e redirecionar para /login', () => {
    cy.get('#btn-logout').click()

    // Após logout, deve redirecionar para login
    cy.url({ timeout: 5000 }).should('include', '/login')

    // O localStorage deve estar limpo (token removido)
    cy.window().then((win) => {
      expect(win.localStorage.getItem('fd_token')).to.be.null
    })
  })
})
