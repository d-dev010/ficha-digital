function fn() {
  // Pega a variável de ambiente 'karate.env' (pode ser 'dev', 'prd', etc)
  var env = karate.env; 
  karate.log('karate.env system property was:', env);

  if (!env) {
    env = 'dev'; // se não passar nada, assume 'dev' (local)
  }

  var config = {
    // Configurações padrão
    apiUrl: 'http://localhost:8080/api'
  };

  if (env == 'prd') {
    // URL da sua API no Render. Substitua pela sua URL real do Render.
    config.apiUrl = 'https://sua-api-ficha-digital.onrender.com/api';
  }

  // Define um timeout maior, especialmente para ambientes cloud como o Render
  karate.configure('connectTimeout', 5000);
  karate.configure('readTimeout', 5000);

  return config;
}
