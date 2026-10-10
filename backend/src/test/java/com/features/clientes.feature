Feature: API de Clientes da Ficha Digital

  Background:
    # A URL base agora é puxada do karate-config.js automaticamente
    * url apiUrl
    
    # 1. Faz o Login usando as credenciais do karate-config.js
    # Em 'dev': usa os valores do próprio arquivo de config
    # Em 'prd': usa os Secrets do GitHub (KARATE_ADMIN_EMAIL / KARATE_ADMIN_PASSWORD)
    * def loginPayload = { email: '#(adminEmail)', senha: '#(adminPassword)' }
    * path 'auth/login'
    * request loginPayload
    * method post
    * status 200
    
    # 2. Pega o Token da resposta e configura no Header para os próximos cenários
    * def token = response.token
    * header Authorization = 'Bearer ' + token

  Scenario: Buscar lista de clientes e validar resposta
    Given path 'clientes'
    When method get
    Then status 200
    # Valida que a resposta é um array
    And match response == '#array'

  Scenario: Criar um novo cliente com sucesso
    Given path 'clientes'
    And request { nome: 'Cliente Teste Karate', telefone: '11999998888', email: 'karate@teste.com' }
    When method post
    Then status 201
    # Valida se o ID foi gerado (não é nulo) e se o nome bate com o que enviamos
    And match response.id == '#notnull'
    And match response.nome == 'Cliente Teste Karate'
