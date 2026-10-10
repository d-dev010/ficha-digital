Feature: API de Clientes da Ficha Digital

  # ============================================================
  # callonce: cria a farmácia + faz login UMA ÚNICA VEZ
  # para toda a suíte de testes, evitando conflito de dados (409)
  # ============================================================

  Background:
    * url apiUrl
    * def setup = callonce read('setup-farmacia.feature')
    * def token = setup.token
    * header Authorization = 'Bearer ' + token

  # ─────────────────────────────────────────────────────────────

  Scenario: Buscar lista de clientes (deve retornar lista vazia para farmácia nova)
    Given path 'clientes'
    When method get
    Then status 200
    # A farmácia acabou de ser criada, a lista deve ser vazia
    And match response.content == '#[]'

  Scenario: Cadastrar um novo cliente com sucesso
    Given path 'clientes'
    And request { nome: 'Cliente Teste Karate', telefone: '11999998888' }
    When method post
    Then status 201
    And match response.id == '#notnull'
    And match response.nome == 'Cliente Teste Karate'
