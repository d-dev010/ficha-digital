Feature: Setup de Farmácia para Testes

  # Este feature é chamado uma única vez (callonce) pelo clientes.feature
  # Cria a farmácia de teste e retorna o token para os cenários

  Scenario: Criar farmácia e obter token de autenticação
    Given url apiUrl
    And path 'farmacias'
    And request
      """
      {
        "nomeFarmacia": "Farmácia Teste Karate",
        "cnpj": "12345678000195",
        "nomeDono": "Dono Teste",
        "emailDono": "dono.karate@teste.com",
        "senhaDono": "Senha@Teste123"
      }
      """
    When method post
    Then status 201

    # Agora faz login com o dono recém-criado
    # Nota: resetamos explicitamente a url para evitar acúmulo de path do Karate
    Given url apiUrl
    And path 'auth/login'
    And request { email: 'dono.karate@teste.com', senha: 'Senha@Teste123' }
    When method post
    Then status 200

    # Expõe o token como propriedade do objeto retornado pelo callonce
    # No clientes.feature: def setup = callonce read(...) => setup.token
    * def token = response.token
