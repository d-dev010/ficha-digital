Feature: API de Clientes da Ficha Digital

  # ============================================================
  # Fluxo realista multi-tenant:
  # 1. Cria uma farmácia de teste (rota pública)
  # 2. Faz login com o usuário DONO criado automaticamente
  # 3. Usa o token com farmaciaId para testar as rotas de Clientes
  # ============================================================

  Background:
    * url apiUrl

    # ── PASSO 1: Criar uma Farmácia de teste ──────────────────
    # Esta rota é pública (não precisa de token)
    * path 'farmacias'
    * request
      """
      {
        "nomeFarmacia": "Farmácia Teste Karate",
        "cnpj": "12345678000195",
        "nomeDono": "Dono Teste",
        "emailDono": "dono.karate@teste.com",
        "senhaDono": "Senha@Teste123"
      }
      """
    * method post
    * status 201

    # ── PASSO 2: Login com o Dono recém-criado ────────────────
    * def loginPayload = { email: 'dono.karate@teste.com', senha: 'Senha@Teste123' }
    * path 'auth/login'
    * request loginPayload
    * method post
    * status 200

    # ── PASSO 3: Salva o Token para os próximos cenários ─────
    * def token = response.token
    * header Authorization = 'Bearer ' + token

  # ─────────────────────────────────────────────────────────────

  Scenario: Buscar lista de clientes (deve retornar lista vazia para farmácia nova)
    Given path 'clientes'
    When method get
    Then status 200
    # A farmácia acabou de ser criada, então a lista de clientes deve ser vazia
    And match response.content == '#[]'

  Scenario: Cadastrar um novo cliente com sucesso
    Given path 'clientes'
    And request { nome: 'Cliente Teste Karate', telefone: '11999998888' }
    When method post
    Then status 201
    And match response.id == '#notnull'
    And match response.nome == 'Cliente Teste Karate'
