// =============================================================================
// SUITE: Autenticação (Login)
// OBJETIVO: Validar os fluxos de login da aplicação Ficha Digital.
//
// Por que testamos isso?
// O login é o "portão" da aplicação. Se ele falha, nenhuma outra
// funcionalidade é acessível. Testar em E2E garante que o frontend,
// o backend e a rede estão todos integrados e funcionando juntos.
// =============================================================================

describe('Autenticação', () => {
  // beforeEach roda antes de CADA teste da suite.
  // Garantimos que sempre começamos na página de login, com estado limpo.
  beforeEach(() => {
    // Limpa tokens salvos para garantir que não há sessão ativa
    cy.clearLocalStorage()
    cy.visit('/login')
  })

  // --------------------------------------------------------------------------
  // TC-01: Renderização da página de login
  // Por que testar isso? Para garantir que a rota /login existe e que
  // os elementos essenciais do formulário são exibidos ao usuário.
  // --------------------------------------------------------------------------
  it('TC-01 | Deve exibir o formulário de login corretamente', () => {
    // Verifica o título da aplicação
    cy.contains('Ficha Digital').should('be.visible')

    // Verifica os campos do formulário pelos IDs definidos no HTML
    cy.get('#login-email').should('be.visible')
    cy.get('#login-senha').should('be.visible')

    // O botão de entrar deve existir, mas estar desabilitado (formulário vazio é inválido)
    cy.get('#btn-entrar').should('be.disabled')
  })

  // --------------------------------------------------------------------------
  // TC-02: Validação de campos obrigatórios
  // Por que testar isso? Garante que a validação do Angular (reactive forms)
  // está funcionando e que o botão só habilita com dados válidos.
  // --------------------------------------------------------------------------
  it('TC-02 | Deve desabilitar o botão de entrar com e-mail inválido', () => {
    // Digita um e-mail com formato inválido
    cy.get('#login-email').type('email-invalido')
    cy.get('#login-senha').type('qualquersenha')

    // O Angular deve manter o botão desabilitado pois o email é inválido
    cy.get('#btn-entrar').should('be.disabled')

    // A mensagem de erro de validação deve aparecer
    cy.contains('E-mail inválido').should('be.visible')
  })

  // --------------------------------------------------------------------------
  // TC-03: Credenciais incorretas
  // Por que testar isso? O backend deve retornar 401/403 para credenciais
  // erradas e o frontend deve exibir uma mensagem de erro ao usuário,
  // em vez de travar ou redirecionar.
  // --------------------------------------------------------------------------
  it('TC-03 | Deve exibir erro ao tentar logar com credenciais inválidas', () => {
    cy.get('#login-email').type('nao_existe@email.com')
    cy.get('#login-senha').type('SenhaErrada123!')

    cy.get('#btn-entrar').click()

    // Após a tentativa falhar, deve aparecer uma mensagem de erro (role="alert")
    // O selector [role="alert"] é mais robusto e acessível
    cy.get('[role="alert"]', { timeout: 8000 }).should('be.visible')

    // E a URL não deve mudar — o usuário permanece no login
    cy.url().should('include', '/login')
  })

  // --------------------------------------------------------------------------
  // TC-04: Login com sucesso (Happy Path)
  // Por que testar isso? É o fluxo mais crítico da aplicação.
  // Garante que um usuário legítimo consegue autenticar e é redirecionado.
  // --------------------------------------------------------------------------
  it('TC-04 | Deve autenticar e redirecionar para /clientes com credenciais válidas', () => {
    cy.get('#login-email').type('sla@gmail.com')
    cy.get('#login-senha').type('@Killa003')

    cy.get('#btn-entrar').click()

    // Após login bem-sucedido, aguarda o redirecionamento para /clientes
    // timeout de 10s para considerar latência de rede
    cy.url({ timeout: 10000 }).should('include', '/clientes')

    // Verifica que a toolbar com o nome do usuário é exibida
    // Isso confirma que o token foi armazenado e o perfil foi carregado
    cy.get('mat-toolbar').should('be.visible')

    // Verifica que o botão de logout está disponível (prova que está autenticado)
    cy.get('#btn-logout').should('be.visible')
  })

  // --------------------------------------------------------------------------
  // TC-05: Proteção de rotas (Auth Guard)
  // Por que testar isso? Garante que o Angular Route Guard está bloqueando
  // acesso não autorizado e redirecionando para o login.
  // --------------------------------------------------------------------------
  it('TC-05 | Deve redirecionar para /login ao tentar acessar rota protegida sem autenticação', () => {
    // Tenta acessar diretamente a rota protegida sem estar logado
    cy.visit('/clientes')

    // O authGuard deve interceptar e redirecionar para login
    cy.url({ timeout: 5000 }).should('include', '/login')
  })
})
