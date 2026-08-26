<h1 align="center">
  <br>
  💊 Ficha Digital
  <br>
</h1>

<h4 align="center">Sistema de controle de fiado/crediário digital para farmácias</h4>

<p align="center">
  <img src="https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" />
  <img src="https://img.shields.io/badge/Spring_Boot-3.3-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white" />
  <img src="https://img.shields.io/badge/Angular-18-DD0031?style=for-the-badge&logo=angular&logoColor=white" />
  <img src="https://img.shields.io/badge/PostgreSQL-16-4169E1?style=for-the-badge&logo=postgresql&logoColor=white" />
  <img src="https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white" />
</p>

<p align="center">
  <a href="#-sobre-o-projeto">Sobre</a> •
  <a href="#-funcionalidades">Funcionalidades</a> •
  <a href="#-arquitetura">Arquitetura</a> •
  <a href="#-tecnologias">Tecnologias</a> •
  <a href="#-como-executar">Como Executar</a> •
  <a href="#-segurança">Segurança</a>
</p>

---

## 📖 Sobre o Projeto

O **Ficha Digital** é uma aplicação web fullstack que substitui a tradicional "caderneta de fiado" de farmácias por um sistema digital moderno, seguro e multi-tenant. Cada farmácia gerencia sua própria carteira de clientes com saldo devedor em tempo real, controle de lançamentos e registro de pagamentos — tudo com autenticação JWT e isolamento rigoroso de dados por tenant.

> Projeto desenvolvido com foco em **boas práticas de engenharia**, **segurança por design** e **performance sob concorrência**.

---

## ✨ Funcionalidades

| Feature | Descrição |
|---|---|
| 🔐 **Autenticação JWT** | Login com token stateless, expiração configurável |
| 👥 **Gestão de Clientes** | Cadastro, busca por nome/CPF/telefone com paginação |
| 📋 **Lançamento de Fiado** | Registro de crédito com histórico por cliente |
| 💰 **Registro de Pagamentos** | Quitação parcial ou total do saldo devedor |
| 📊 **Extrato do Cliente** | Histórico completo de lançamentos e pagamentos |
| 🏪 **Multi-Tenant** | Isolamento total de dados entre farmácias |
| 🔒 **Privacy by Design** | CPF mascarado nas listagens, completo apenas no detalhe |
| 📐 **Dashboard Gerencial** | Visão geral exclusiva para donos de farmácia (role-based) |
| ⚡ **Busca com Debounce** | Pesquisa reativa com 350ms de debounce no frontend |

---

## 🏗️ Arquitetura

```
ficha-digital/
├── backend/                  # API REST — Spring Boot 3
│   └── src/main/java/com/fichadigital/
│       ├── auth/             # Autenticação e geração de JWT
│       ├── cliente/          # CRUD e busca de clientes
│       ├── lancamento/       # Lançamentos de fiado (optimistic locking)
│       ├── pagamento/        # Registro de pagamentos
│       ├── extrato/          # Histórico consolidado
│       ├── farmacia/         # Entidade da farmácia (tenant)
│       ├── usuario/          # Usuários e roles (DONO / ATENDENTE)
│       ├── security/         # Filtros JWT e configuração Spring Security
│       └── config/           # CORS, OpenAPI/Swagger, etc.
│
├── frontend/                 # SPA — Angular 18 + Angular Material
│   └── src/app/
│       ├── core/             # Auth service, guards, interceptors, models
│       ├── features/
│       │   ├── login/        # Tela de login
│       │   ├── clientes/     # Busca, detalhe e extrato de clientes
│       │   └── dashboard/    # Painel gerencial (role DONO)
│       └── shared/           # Pipes reutilizáveis (ex: CurrencyBrPipe)
│
└── docker-compose.yml        # Orquestra API + PostgreSQL
```

### Fluxo da Aplicação

```
[Angular SPA] ──JWT──▶ [Spring Boot API] ──JPA──▶ [PostgreSQL]
     │                        │
     │                  Spring Security
     │                  (JWT Filter + Role Guard)
     │
  Angular Router
  (authGuard + roleGuard)
```

---

## 🛠️ Tecnologias

### Backend
| Tecnologia | Versão | Uso |
|---|---|---|
| Java | 17 | Linguagem principal |
| Spring Boot | 3.3.4 | Framework web |
| Spring Security | 6 | Autenticação e autorização |
| Spring Data JPA | — | Persistência e repositórios |
| jjwt | 0.12.5 | Geração e validação de tokens JWT |
| Flyway | — | Migrations de banco de dados |
| Lombok | — | Redução de boilerplate |
| Springdoc OpenAPI | 2.6.0 | Documentação Swagger UI |
| Bucket4j | 8.10.1 | Rate limiting na API |
| PostgreSQL | 16 | Banco de dados relacional |

### Frontend
| Tecnologia | Versão | Uso |
|---|---|---|
| Angular | 18 | Framework SPA |
| Angular Material | 18 | Componentes de UI |
| TypeScript | 5.5 | Tipagem estática |
| RxJS | 7.8 | Programação reativa |
| Angular Signals | — | Gerenciamento de estado local |

### Infraestrutura
| Tecnologia | Uso |
|---|---|
| Docker + Docker Compose | Containerização do backend e banco |
| Maven | Build do backend |

---

## 🔐 Segurança

O projeto implementa diversas boas práticas de segurança:

- **Multi-Tenant por JWT**: O `farmaciaId` **nunca** vem do body da requisição. Ele é sempre extraído do token JWT no `SecurityContext`, impedindo que um usuário acesse dados de outra farmácia.

- **Privacy by Design**: O CPF é **mascarado** (`123.***.***-00`) em todas as listagens. O CPF completo é exibido apenas na tela de detalhe do cliente.

- **Optimistic Locking no Saldo**: O `saldo_devedor` é atualizado atomicamente via `@Transactional`. A entidade `Cliente` usa `@Version` — em caso de escrita concorrente, a operação é repetida automaticamente (até 3 tentativas), eliminando locks pessimistas no banco.

- **Rate Limiting**: Proteção contra abusos na API com **Bucket4j**.

- **Guards no Frontend**: `authGuard` protege todas as rotas autenticadas; `roleGuard('DONO')` restringe o dashboard apenas a donos de farmácia.

---

## 🚀 Como Executar

### Pré-requisitos
- [Docker](https://www.docker.com/) e Docker Compose
- [Node.js](https://nodejs.org/) 18+ e npm

### 1. Clone o repositório
```bash
git clone https://github.com/d-dev010/ficha-digital.git
cd ficha-digital
```

### 2. Configure as variáveis de ambiente
```bash
cp .env.example .env
# Edite o .env com suas configurações
```

```env
POSTGRES_DB=ficha_digital
POSTGRES_USER=postgres
POSTGRES_PASSWORD=sua_senha_segura
JWT_SECRET=sua_chave_secreta_com_pelo_menos_256_bits
JWT_EXPIRATION_MS=28800000
```

### 3. Suba o Backend e o Banco com Docker
```bash
docker compose up -d
```

A API estará disponível em `http://localhost:8080`  
Swagger UI: `http://localhost:8080/swagger-ui.html`

### 4. Suba o Frontend
```bash
cd frontend
npm install
npm start
```

O frontend estará disponível em `http://localhost:4200`

---

## 📡 Principais Endpoints da API

| Método | Endpoint | Descrição | Role |
|---|---|---|---|
| `POST` | `/auth/login` | Autenticação e geração de JWT | Público |
| `GET` | `/clientes?termo=&page=` | Busca paginada de clientes | ATENDENTE, DONO |
| `POST` | `/clientes` | Cadastra novo cliente | ATENDENTE, DONO |
| `GET` | `/clientes/{id}` | Detalhe do cliente (CPF completo) | ATENDENTE, DONO |
| `PATCH` | `/clientes/{id}/telefone` | Atualiza telefone | ATENDENTE, DONO |
| `POST` | `/lancamentos` | Lança um fiado | ATENDENTE, DONO |
| `POST` | `/pagamentos` | Registra um pagamento | ATENDENTE, DONO |
| `GET` | `/clientes/{id}/extrato` | Extrato do cliente | ATENDENTE, DONO |

> 📄 Documentação completa disponível via Swagger UI em `/swagger-ui.html`

---

## 📄 Licença

Este projeto está sob a licença MIT.

---

<p align="center">
  Feito com ❤️ por <strong>Davi</strong>
</p>
