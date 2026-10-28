<h1 align="center">
  <br>
  💊 Ficha Digital
  <br>
</h1>

<h4 align="center">Sistema SaaS Multi-Tenant de controle de fiado e crediário digital para farmácias</h4>

<p align="center">
  <img src="https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" />
  <img src="https://img.shields.io/badge/Spring_Boot-3.3.4-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white" />
  <img src="https://img.shields.io/badge/Angular-18-DD0031?style=for-the-badge&logo=angular&logoColor=white" />
  <img src="https://img.shields.io/badge/PostgreSQL-16-4169E1?style=for-the-badge&logo=postgresql&logoColor=white" />
  <img src="https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white" />
  <img src="https://img.shields.io/badge/Cypress-15-17202C?style=for-the-badge&logo=cypress&logoColor=white" />
  <img src="https://img.shields.io/badge/Karate-1.4.1-D32F2F?style=for-the-badge" />
</p>

<p align="center">
  <a href="#-sobre-o-projeto">Sobre</a> •
  <a href="#-funcionalidades">Funcionalidades</a> •
  <a href="#-arquitetura">Arquitetura</a> •
  <a href="#-tecnologias">Tecnologias</a> •
  <a href="#-segurança">Segurança</a> •
  <a href="#-principais-endpoints-da-api">Endpoints</a> •
  <a href="#-testes-e-qa">Testes & QA</a> •
  <a href="#-como-executar">Como Executar</a> •
  <a href="#-variáveis-de-ambiente">Variáveis de Ambiente</a>
</p>

---

## 📖 Sobre o Projeto

O **Ficha Digital** é uma aplicação web fullstack e multi-tenant desenvolvida para modernizar a tradicional "caderneta de fiado" de farmácias e drogarias, transformando-a em uma plataforma digital ágil, intuitiva e altamente segura.

Cada farmácia opera de forma completamente isolada, gerenciando sua própria carteira de clientes, saldos devedores em tempo real, lançamentos de compras com identificação de retirada, controle de pagamentos e relatórios contábeis. A plataforma conta também com um painel de administração global (**Super Admin**) para controle e monitoramento de estabelecimentos na rede.

> 🛡️ **Engenharia de Software de Alto Nível**: Arquitetura projetada com isolamento rigoroso por tenant no token JWT, controle de concorrência com Lock Otimista, proteção de memória com queries nativas de alta performance, auditoria de segurança comprovada e esteira automatizada de testes de integração e ponta a ponta (CI/CD).

---

## ✨ Funcionalidades

### 👥 Gestão de Clientes & Fichas
- **Cadastro Completo**: Nome, telefone, endereço residencial físico e CPF (opcional, com validação de formato e unicidade por farmácia).
- **Busca Inteligente com Unaccent**: Pesquisa instantânea por nome, telefone ou CPF no PostgreSQL, com suporte a remoção de acentos (ex: *"josé"* encontra *"José"*), aceleração por índice funcional e debounce no frontend (350ms).
- **Privacy by Design**: CPF mascarado (`123.***.***-00`) em todas as listagens públicas, sendo exibido por completo exclusivamente na tela de detalhes do cliente.
- **Edição e Exclusão Segura**: Atualização de telefone e exclusão definitiva de clientes (com modal de confirmação de segurança e limpeza em cascata, restrito ao perfil `DONO`).
- **Adequação à LGPD**: Estrutura de banco e modelo com consentimento de dados, data de autorização e flags de anonimização.

### 📋 Operações Financeiras & Fiado
- **Lançamento de Fiado (Compras)**: Registro do débito com valor, descrição detalhada, data/hora exata e registro de quem retirou o item (caso retirado por terceiros ou familiares).
- **Registro de Pagamentos**: Quitação parcial ou total da dívida com abatimento em tempo real e atualização atômica do saldo.
- **Lock Otimista no Saldo**: Controle de concorrência via `@Version` em nível de entidade JPA com retentativas automáticas, evitando inconsistências em caixas simultâneos.
- **Extrato Paginado Unificado**: Histórico cronológico completo consolidando lançamentos e pagamentos via query nativa otimizada (`UNION ALL`), com paginação direta no banco para evitar estouro de memória (OOM).

### 📊 Relatórios & Exportação
- **Exportação CSV do Extrato do Cliente**: Download de movimentações de um cliente específico com opção de filtro por mês e ano.
- **Relatório Financeiro Geral da Farmácia**: Exportação consolidada de todas as vendas e recebimentos em formato CSV, com codificação UTF-8 com BOM e separador `;` compatível de imediato com o Microsoft Excel no Brasil.

### 📈 Painéis & Níveis de Acesso
- **Super Admin (`SUPER_ADMIN`)**: Visão de plataforma para gerenciamento de todas as farmácias, criação de administradores de loja, redefinição de senhas e bloqueio/ativação de contas.
- **Painel do Dono (`DONO`)**: Métricas em tempo real (total de clientes ativos e montante total a receber), cadastro de funcionários atendentes e exportação de relatórios gerais.
- **Atendente (`ATENDENTE`)**: Operação do balcão no dia a dia (busca, novos clientes, lançamentos de fiado, baixas de pagamentos e consulta de extratos).

---

## 🏗️ Arquitetura

```
ficha-digital/
├── .github/
│   └── workflows/
│       ├── qa-pipeline.yml   # CI/CD: sobe Docker e roda testes Karate da API
│       └── keep-alive.yml    # Cron para manter a API aquecida no cloud free-tier
│
├── backend/                  # API REST — Spring Boot 3.3.4 (Java 17)
│   ├── src/main/java/com/fichadigital/
│   │   ├── admin/            # Gestão global de farmácias e usuários (SUPER_ADMIN)
│   │   ├── auth/             # Login, credenciais e geração de JWT
│   │   ├── cliente/          # CRUD, busca unaccent, paginação e exclusão
│   │   ├── config/           # SecurityConfig, CORS, OpenAPI/Swagger
│   │   ├── extrato/          # Histórico unificado nativo (UNION ALL) e CSV
│   │   ├── farmacia/         # Cadastro de tenant e resumo gerencial
│   │   ├── health/           # Health check público (/health)
│   │   ├── lancamento/       # Débito de fiado e rastreio de quem retirou
│   │   ├── pagamento/        # Amortização e quitação de saldo
│   │   ├── relatorio/        # Exportação geral de relatórios em CSV
│   │   ├── security/         # JwtAuthFilter, RateLimitFilter e SecurityUtils
│   │   └── usuario/          # Gestão de usuários, perfis e credenciais
│   ├── src/main/resources/
│   │   ├── db/migration/     # 11 migrações versionadas com Flyway
│   │   └── application.yml   # Configurações do Spring Boot
│   └── src/test/             # Testes unitários e features Karate BDD
│
├── frontend/                 # SPA — Angular 18 (Standalone Components + Signals)
│   ├── cypress/              # Testes End-to-End (E2E) com Cypress
│   └── src/app/
│       ├── core/             # Auth service, HTTP Interceptors, Guards e Models
│       ├── features/
│       │   ├── admin/        # Painel da plataforma (Super Admin)
│       │   ├── clientes/     # Busca, perfil, extrato e diálogos de ação
│       │   ├── dashboard/    # Visão gerencial da farmácia e gestão de equipe
│       │   └── login/        # Tela de autenticação responsiva
│       └── shared/           # Pipes (CurrencyBr) e componentes compartilhados
│
├── docs/
│   └── security-audit/       # Relatório de auditoria e conformidade de segurança
│
└── docker-compose.yml        # Orquestração da API e do PostgreSQL 16
```

---

## 🛠️ Tecnologias

### Backend
| Tecnologia | Versão | Descrição |
|---|---|---|
| **Java** | 17 | Linguagem LTS base do ecossistema |
| **Spring Boot** | 3.3.4 | Framework base para construção da API REST |
| **Spring Security** | 6.x | Autenticação stateless e controle RBAC via anotações |
| **Spring Data JPA / Hibernate** | — | Mapeamento objeto-relacional e queries nativas otimizadas |
| **PostgreSQL** | 16 | Banco relacional com extensão `unaccent` e índices funcionais |
| **Flyway** | — | Versionamento automatizado de schema e migrations de banco |
| **JJWT (io.jsonwebtoken)** | 0.12.5 | Emissão e validação criptográfica de tokens JWT HMAC-SHA256 |
| **Bucket4j + Caffeine** | 8.10.1 | Rate limiting granular por IP com evicção automática em cache |
| **Springdoc OpenAPI** | 2.6.0 | Documentação interativa de endpoints com Swagger UI |
| **Lombok** | — | Eliminação de boilerplate de código Java |

### Frontend
| Tecnologia | Versão | Descrição |
|---|---|---|
| **Angular** | 18 | Framework SPA com arquitetura standalone |
| **Angular Material** | 18 | Design system acessível e consistente |
| **TypeScript** | 5.5 | Tipagem estática e segurança em tempo de compilação |
| **RxJS** | 7.8 | Programação reativa para fluxos assíncronos e debounce |
| **Angular Signals** | — | Gerenciamento de estado reativo e granular |

### Testes & Qualidade (QA)
| Ferramenta | Escopo | Descrição |
|---|---|---|
| **Karate Framework** (1.4.1) | API / BDD | Testes de integração em formato Gherkin/BDD |
| **Cypress** (15.x) | E2E | Testes de ponta a ponta simulando a experiência do usuário |
| **JUnit 5 + Mockito** | Backend Unit | Testes unitários de serviços e regras de negócio |
| **GitHub Actions** | CI/CD | Pipeline automatizado de validação contínua e Docker |

---

## 🔐 Segurança

O Ficha Digital passou por auditoria completa de segurança (detalhada em `docs/security-audit/relatorio-auditoria-seguranca.md`). Entre as proteções aplicadas:

1. **Multi-Tenancy Hermético (RNF03)**:
   - O identificador da farmácia (`farmaciaId`) **nunca** é aceito no corpo ou parâmetro livre de requisições de clientes comuns.
   - O `SecurityUtils` extrai o `farmaciaId` diretamente dos *claims* assinados do JWT no `SecurityContext`.
2. **Defesa em Camadas contra IDOR**:
   - Todas as operações em clientes, lançamentos e pagamentos verificam explicitamente a posse do recurso pela farmácia autenticada (`WHERE id = :id AND farmacia_id = :farmaciaId`).
3. **Controle de Concorrência Otimista (RNF10)**:
   - O campo `versao` (`@Version`) na entidade `Cliente` impede problemas de *race condition* ou sobreescrita cega de saldos devedores em acessos concomitantes.
4. **Rate Limiting com Evicção Segura**:
   - Utilização de **Bucket4j** integrado a um cache **Caffeine** com tempo de expiração (TTL), prevenindo ataques de negação de serviço (DoS) e vazamento de memória por acúmulo de chaves de IP.
5. **Prevenção de Injeção de Código**:
   - Queries nativas usam estritamente *Prepared Statements* com parâmetros nomeados.
   - O frontend Angular faz sanitização automática via `DomSanitizer`, sem uso de `innerHTML` ou desvios de segurança.

---

## 📡 Principais Endpoints da API

### Autenticação & Farmácia
| Método | Endpoint | Descrição | Acesso |
|---|---|---|---|
| `POST` | `/auth/login` | Realiza login e emite o token JWT com perfil | Público |
| `POST` | `/farmacias` | Cadastro inicial da farmácia e de seu usuário DONO | Público |
| `GET` | `/farmacias/resumo` | Métricas gerais: clientes ativos e saldo a receber | `DONO` |
| `POST` | `/usuarios` | Cadastro de novos funcionários atendentes | `DONO` |
| `GET` | `/health` | Verificação de integridade e status da API | Público |

### Gestão de Clientes & Movimentações
| Método | Endpoint | Descrição | Acesso |
|---|---|---|---|
| `GET` | `/clientes?busca=&page=&size=` | Busca paginada (nome, CPF, telefone) com unaccent | `ATENDENTE`, `DONO` |
| `POST` | `/clientes` | Cadastra novo cliente (com endereço e CPF opcional) | `ATENDENTE`, `DONO` |
| `GET` | `/clientes/{id}` | Detalhes completos do cliente (com CPF desmascarado) | `ATENDENTE`, `DONO` |
| `PATCH` | `/clientes/{id}/telefone` | Atualização do número de telefone | `ATENDENTE`, `DONO` |
| `DELETE` | `/clientes/{id}` | Exclusão do cliente e seu histórico associado | `DONO` |
| `POST` | `/clientes/{id}/lancamentos` | Registra nova compra/fiado (com identificação de retirada) | `ATENDENTE`, `DONO` |
| `POST` | `/clientes/{id}/pagamentos` | Registra quitação ou amortização de débito | `ATENDENTE`, `DONO` |
| `GET` | `/clientes/{id}/extrato` | Extrato paginado com histórico unificado | `ATENDENTE`, `DONO` |
| `GET` | `/clientes/{id}/extrato/csv` | Download do extrato do cliente em CSV | `ATENDENTE`, `DONO` |
| `GET` | `/relatorio/csv` | Download do relatório financeiro geral da farmácia | `DONO` |

### Administração Global (Super Admin)
| Método | Endpoint | Descrição | Acesso |
|---|---|---|---|
| `GET` | `/admin/farmacias` | Listagem global de todas as farmácias cadastradas | `SUPER_ADMIN` |
| `POST` | `/admin/farmacias` | Criação administrativa de estabelecimentos | `SUPER_ADMIN` |
| `DELETE` | `/admin/farmacias/{id}` | Remoção de farmácia da plataforma | `SUPER_ADMIN` |
| `GET` | `/admin/usuarios` | Consulta de usuários com filtro por farmácia | `SUPER_ADMIN` |
| `POST` | `/admin/usuarios` | Criação direta de usuários e perfis | `SUPER_ADMIN` |
| `PATCH` | `/admin/usuarios/{id}/senha` | Redefinição forçada de credencial de usuário | `SUPER_ADMIN` |
| `PATCH` | `/admin/usuarios/{id}/status` | Ativação ou desativação de usuário | `SUPER_ADMIN` |
| `DELETE` | `/admin/usuarios/{id}` | Exclusão de usuário do sistema | `SUPER_ADMIN` |

> 📑 A documentação Swagger interativa completa pode ser acessada em: `http://localhost:8080/swagger-ui.html`

---

## 🧪 Testes e QA

### 1. Testes de API com Karate (BDD)
Os cenários de integração simulam jornadas reais de ponta a ponta (login, cadastro de clientes, validação de regras multi-tenant e concorrência):
```bash
cd backend
mvn test -Dtest=KarateTest -Dkarate.env=dev
```

### 2. Testes de Interface com Cypress (E2E)
Executa a validação visual e comportamental no navegador:
```bash
cd frontend
# Modo interativo com interface gráfica
npm run cypress:open

# Modo headless para integração contínua
npm run cypress:run
```

### 3. Testes Unitários do Backend
```bash
cd backend
mvn test
```

---

## 🚀 Como Executar

### Pré-requisitos
- [Docker](https://www.docker.com/) e Docker Compose instalados.
- [Node.js](https://nodejs.org/) versão 18+ e npm.
- [Java JDK](https://adoptium.net/) versão 17+ e Maven (opcional, para execução fora de contêineres).

### 1. Clonar o Repositório
```bash
git clone https://github.com/d-dev010/ficha-digital.git
cd ficha-digital
```

### 2. Configurar o Ambiente
Copie o arquivo de exemplo e ajuste as chaves caso necessário:
```bash
cp .env.example .env
```

### 3. Iniciar o Backend e Banco de Dados com Docker
```bash
docker compose up -d --build
```
- API Spring Boot: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Health check: `http://localhost:8080/health`

### 4. Iniciar o Frontend Angular
Em um terminal separado:
```bash
cd frontend
npm install
npm start
```
- Aplicação Web: `http://localhost:4200`

---

## ⚙️ Variáveis de Ambiente

As configurações são definidas no arquivo `.env` na raiz do projeto:

```env
# Banco de Dados PostgreSQL
POSTGRES_DB=ficha_digital
POSTGRES_USER=postgres
POSTGRES_PASSWORD=sua_senha_segura

# Autenticação e Segurança JWT
JWT_SECRET=sua_chave_secreta_com_pelo_menos_256_bits_gerada_aleatoriamente
JWT_EXPIRATION_MS=28800000

# Credenciais Iniciais do Super Administrador
ADMIN_EMAIL=admin@suafarmacia.com
ADMIN_PASSWORD=SenhaForteDe16CaracteresOuMais!

# CORS (URL de acesso do Frontend)
CORS_ALLOWED_ORIGIN=http://localhost:4200
```

---

## 📄 Licença

Este projeto é distribuído sob os termos da licença **MIT**. Consulte o arquivo de licença para mais detalhes.

---

<p align="center">
  Desenvolvido com foco em excelência técnica, confiabilidade e segurança.
</p>
