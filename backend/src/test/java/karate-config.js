function fn() {
  var env = karate.env;
  karate.log('karate.env system property was:', env);

  if (!env) {
    env = 'dev';
  }

  var config = {
    // Ambiente dev (local ou Docker do GitHub CI)
    // Credenciais espelham o .env.example para o Docker conseguir autenticar
    apiUrl: 'http://localhost:8080/api',
    adminEmail: 'admin@suafarmacia.com',
    adminPassword: 'SenhaForteDe16CaracteresOuMais!'
  };

  if (env == 'prd') {
    // Apontado para a API no Render via secrets do GitHub
    config.apiUrl = java.lang.System.getenv('KARATE_API_URL') || 'https://sua-api.onrender.com/api';
    config.adminEmail = java.lang.System.getenv('KARATE_ADMIN_EMAIL') || '';
    config.adminPassword = java.lang.System.getenv('KARATE_ADMIN_PASSWORD') || '';
  }

  // Timeout maior para ambientes lentos (Render "acorda" o serviço)
  karate.configure('connectTimeout', 15000);
  karate.configure('readTimeout', 15000);

  return config;
}
