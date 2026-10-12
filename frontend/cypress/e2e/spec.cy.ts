// =============================================================================
// SUITE: Smoke Tests
// OBJETIVO: Verificação rápida de que a aplicação está "de pé".
//
// Smoke tests são a primeira barreira — se eles falham, nem adianta
// rodar as suites completas. Devem ser simples e ultrarrápidos.
// =============================================================================

describe('Smoke Tests - Aplicação Ficha Digital', () => {
  it('TC-00 | A aplicação deve carregar e redirecionar para /login sem autenticação', () => {
    cy.clearLocalStorage()
    cy.visit('/')

    // Sem token, deve redirecionar para login
    cy.url({ timeout: 8000 }).should('include', '/login')

    // O body deve estar visível (página carregou sem crash de JS)
    cy.get('body').should('be.visible')

    // O título do sistema deve estar na página
    cy.contains('Ficha Digital').should('be.visible')
  })
})

