# Relatório de Auditoria de Segurança — Ficha Digital

**Data:** 12 de Outubro de 2026  
**Status da Auditoria:** Concluída  
**Escopo Auditado:**  
- Backend: Java 17, Spring Boot 3.3.4, Spring Security 6, Spring Data JPA, JJWT 0.12.5, Bucket4j 8.10, Caffeine Cache
- Frontend: Angular 18 (Standalone Components, Signals, Route Guards, Angular Material)
- Banco de Dados: PostgreSQL 16 (Neon Serverless DB / Docker Local) com Migrações Flyway
- Infraestrutura & CI/CD: Dockerfile, Docker Compose, GitHub Actions (`qa-pipeline.yml`, `keep-alive.yml`)

---

## 1. Nota Metodológica e Mapeamento da Stack

A auditoria foi executada analisando o código-fonte estático, mapeamento de permissões, fluxos de autenticação, configurações de infraestrutura e persistência de dados. Cada categoria foi mapeada para a arquitetura tecnológica específica do projeto:

| Categoria Auditada | Mapeamento para a Stack do Projeto |
| :--- | :--- |
| **1. Banco Sem Tranca (Isolamento de Inquilino/Tenant)** | **Single-Database com Shared-Schema**. Como o PostgreSQL não possui RLS (*Row-Level Security*) ativado nas tabelas, o isolamento é puramente lógico (nível de aplicação), dependendo do `JwtAuthFilter` extrair `farmaciaId` do JWT e os repositories/services utilizarem `SecurityUtils.farmaciaId()`. Auditamos se há queries ou endpoints que operem sem esse filtro. |
| **2. Permissão Definida no Navegador** | Cruzamento sistemático entre os guards do Angular (`roleGuard('DONO')`, `roleGuard('SUPER_ADMIN')`, diretivas `@if (auth.perfil() === ...)`) e as anotações `@PreAuthorize` e regras de `SecurityConfig` no Spring Security. Verificação de consistência e garantia de que o servidor rejeita requisições não autorizadas. |
| **3. IDOR (Insecure Direct Object Reference)** | Varredura de **todos os manipuladores de rota** que recebem identificadores de entidades nos paths ou payloads (`/clientes/{id}`, `/clientes/{clienteId}/lancamentos`, `/admin/usuarios/{usuarioId}`, etc.), verificando se o registro pertence estritamente à farmácia do usuário autenticado. |
| **4. Chaves Expostas (Hardcode & Defaults)** | Auditoria de segredos em código, `application.yml`, logs via `System.out.println`, scripts de teste (`karate-config.js`), arquivos `.env.example`, Docker e workflows de CI/CD. |
| **5. Inputs Sem Tratamento (XSS & Spoofing)** | No frontend: verificação de `innerHTML`, `[innerHTML]`, `bypassSecurityTrust*` e links dinâmicos. No backend: validação de DTOs (`@Valid`, constraints Jakarta), manipulação de cabeçalhos controlados pelo usuário (`X-Forwarded-For` no rate limiting) e sanitização de dados persistidos. |

---

## 2. Resumo Executivo

Durante a auditoria sistemática, foram identificados **6 achados acionáveis** (sendo 2 de severidade Alta, 2 Média, 1 Baixa e 1 Informativa), além de **6 pontos fortes comprovados** de segurança arquitetural.

### Tabela Resumo de Severidade

| Severidade | Quantidade | Cor de Referência | Status de Risco |
| :--- | :---: | :---: | :--- |
| **Crítica** | 0 | `#B91C1C` | Nenhuma falha de exploração remota sem autenticação imediata. |
| **Alta** | 2 | `#EA580C` | Risco significativo: Bypass de Rate Limiting e Vazamento de Senha em Logs. |
| **Média** | 2 | `#D97706` | Defaults inseguros e falta de validação em campos livres (risco de DoS/Crash). |
| **Baixa** | 1 | `#2563EB` | Falta de trava de auto-deleção no painel Super Admin. |
| **Informativa** | 1 | `#6B7280` | Divergência de matriz de permissões entre frontend e backend para Super Admin. |
| **Pontos Fortes** | 6 | `#059669` | Mecanismos robustos já implementados e validados. |

---

## 3. Pontos Fortes e Riscos Centrais

### Pontos Fortes Verificados no Código Real
1. **Isolamento de Tenant Rigoroso nas Operações de Negócio (RNF03):**  
   - Em [`ClienteRepository.java`](file:///c:/Users/davib/OneDrive/Documents/ficha%20digital/backend/src/main/java/com/fichadigital/cliente/ClienteRepository.java), todas as consultas (`buscar`, `findByIdAndFarmaciaId`, `contarClientesAtivos`, `somarSaldoDevedor`) filtram expressamente por `farmacia_id`.
   - Em [`LancamentoService.java:86`](file:///c:/Users/davib/OneDrive/Documents/ficha%20digital/backend/src/main/java/com/fichadigital/lancamento/LancamentoService.java#L86) e [`PagamentoService.java:80`](file:///c:/Users/davib/OneDrive/Documents/ficha%20digital/backend/src/main/java/com/fichadigital/pagamento/PagamentoService.java#L80), as operações de débito e crédito sempre consultam `findByIdAndFarmaciaId` antes de qualquer alteração de saldo.
   - Em [`ExtratoController.java:71,84`](file:///c:/Users/davib/OneDrive/Documents/ficha%20digital/backend/src/main/java/com/fichadigital/extrato/ExtratoController.java#L71), a query nativa `UNION ALL` isola estritamente `c.id = :clienteId AND c.farmacia_id = :farmaciaId` em ambas as subconsultas.
2. **Proteção Criptográfica de Senhas (RNF01):**  
   - O Spring Security utiliza `BCryptPasswordEncoder` com strength 10.
   - Em nenhum momento senhas em texto puro são salvas no banco. DTOs de retorno (`UsuarioResponse`, `FarmaciaResponse`, `TokenResponse`) nunca serializam o campo `senhaHash`.
3. **Fail-Fast e Validação de Entropia no Segredo JWT:**  
   - Em [`JwtService.java:41-51`](file:///c:/Users/davib/OneDrive/Documents/ficha%20digital/backend/src/main/java/com/fichadigital/security/JwtService.java#L41-L51), a aplicação valida em tempo de inicialização se o `jwt.secret` possui no mínimo 32 bytes (256 bits para HMAC-SHA256), recusando strings vazias ou fracas.
4. **Prevenção Completa de XSS no Frontend:**  
   - No frontend Angular 18, todos os templates usam interpolação segura `{{ }}` que passa automaticamente pelo `DomSanitizer`. Não há nenhuma ocorrência de `innerHTML`, `[innerHTML]` ou chamadas a `bypassSecurityTrust*`.
5. **Prevenção de SQL Injection:**  
   - Todas as queries dinâmicas e nativas utilizam parâmetros nomeados (`:farmaciaId`, `:termo`, etc.), garantindo o uso de Prepared Statements.
6. **Integridade Transacional e Lock Otimista (RNF10):**  
   - A entidade `Cliente` utiliza `@Version` para detecção de colisões de escrita concorrente em pagamentos e lançamentos, evitando condições de corrida em saldos devedores.

### Riscos Centrais Identificados
1. **Bypass Trivial de Proteção de Brute Force:** O mecanismo de rate limiting do login depende do primeiro IP do cabeçalho `X-Forwarded-For`, permitindo que um atacante falsifique o cabeçalho e realize ataques de força bruta ilimitados contra contas de administradores e donos.
2. **Vazamento de Credencial em Observabilidade:** O `AdminInitializer` imprime no `stdout` os três primeiros caracteres da senha de produção do Super Admin a cada inicialização do container.
3. **Ausência de Defesa em Profundidade no Banco:** O PostgreSQL não possui RLS ativado; uma única query criada no futuro sem `farmacia_id` causará vazamento de dados de múltiplos inquilinos.

---

## 4. Tabela de Achados Detalhados

| Severidade | Arquivo e Linha(s) | Categoria | Descrição Sucinta |
| :--- | :--- | :--- | :--- |
| <span style="background:#EA580C;color:#fff;padding:2px 8px;border-radius:4px;font-weight:bold;">ALTA</span> | [`AuthService.java:50-55, 73-77`](file:///c:/Users/davib/OneDrive/Documents/ficha%20digital/backend/src/main/java/com/fichadigital/auth/AuthService.java#L50-L55) | 5. Inputs / Spoofing | Bypass de rate limit no login via falsificação de `X-Forwarded-For`. |
| <span style="background:#EA580C;color:#fff;padding:2px 8px;border-radius:4px;font-weight:bold;">ALTA</span> | [`AdminInitializer.java:38-43`](file:///c:/Users/davib/OneDrive/Documents/ficha%20digital/backend/src/main/java/com/fichadigital/config/AdminInitializer.java#L38-L43) | 4. Chaves Expostas | Impressão no log/stdout do e-mail e prefixo da senha de produção do Super Admin. |
| <span style="background:#D97706;color:#fff;padding:2px 8px;border-radius:4px;font-weight:bold;">MÉDIA</span> | [`AdminInitializer.java:24-25`](file:///c:/Users/davib/OneDrive/Documents/ficha%20digital/backend/src/main/java/com/fichadigital/config/AdminInitializer.java#L24-L25) e [`karate-config.js:13-14`](file:///c:/Users/davib/OneDrive/Documents/ficha%20digital/backend/src/test/java/karate-config.js#L13-L14) | 4. Chaves Expostas | E-mail de admin com fallback público e credenciais padrão espelhadas no CI. |
| <span style="background:#D97706;color:#fff;padding:2px 8px;border-radius:4px;font-weight:bold;">MÉDIA</span> | [`ClienteController.java:91-97`](file:///c:/Users/davib/OneDrive/Documents/ficha%20digital/backend/src/main/java/com/fichadigital/cliente/ClienteController.java#L91-L97) e [`LancarFiadoRequest.java:17,20`](file:///c:/Users/davib/OneDrive/Documents/ficha%20digital/backend/src/main/java/com/fichadigital/lancamento/LancarFiadoRequest.java#L17) | 5. Inputs Sem Tratamento | DTO sem `@Valid` em `PATCH /clientes/{id}/telefone` e campos sem limite de tamanho. |
| <span style="background:#2563EB;color:#fff;padding:2px 8px;border-radius:4px;font-weight:bold;">BAIXA</span> | [`AdminService.java:101-115`](file:///c:/Users/davib/OneDrive/Documents/ficha%20digital/backend/src/main/java/com/fichadigital/admin/AdminService.java#L101-L115) | 3. IDOR / Controle de Objeto | Ausência de verificação contra auto-desativação e auto-exclusão do Super Admin logado. |
| <span style="background:#6B7280;color:#fff;padding:2px 8px;border-radius:4px;font-weight:bold;">INFO</span> | [`role.guard.ts:18`](file:///c:/Users/davib/OneDrive/Documents/ficha%20digital/frontend/src/app/core/auth/role.guard.ts#L18) e [`FarmaciaController.java:45`](file:///c:/Users/davib/OneDrive/Documents/ficha%20digital/backend/src/main/java/com/fichadigital/farmacia/FarmaciaController.java#L45) | 2. Permissão no Navegador | Descompasso de permissão: frontend autoriza `SUPER_ADMIN` no Dashboard, mas backend rejeita com 403. |

---

## 5. Análise Detalhada dos Achados

### Achado 1: Bypass de Rate Limiting no Login via Spoofing de Cabeçalho HTTP
- **Arquivo:** `backend/src/main/java/com/fichadigital/auth/AuthService.java`
- **Linhas:** 49–55 e 73–77
- **Código:**
  ```java
  private String getClientIp() {
      String xfHeader = httpServletRequest.getHeader("X-Forwarded-For");
      if (xfHeader == null || xfHeader.isEmpty()) {
          return httpServletRequest.getRemoteAddr();
      }
      return xfHeader.split(",")[0].trim();
  }
  ```
- **Por que é explorável:** O método `getClientIp()` lê diretamente o primeiro item do cabeçalho `X-Forwarded-For` fornecido pelo cliente. Qualquer invasor pode enviar uma requisição HTTP contendo `X-Forwarded-For: 203.0.113.1`, depois `203.0.113.2`, etc. A cada requisição com IP falso, o Bucket4j aloca uma nova chave no cache e o rate limiting de 5 tentativas por 5 minutos é totalmente contornado. Como o rate limiting NÃO é feito por e-mail, um atacante pode tentar milhares de senhas contra a conta `admin@fichadigital.com` sem ser bloqueado.
- **Severidade:** **ALTA** (CWE-307 / CWE-345).

---

### Achado 2: Vazamento de Prefixo de Senha de Produção e E-mail de Administrador em Logs
- **Arquivo:** `backend/src/main/java/com/fichadigital/config/AdminInitializer.java`
- **Linhas:** 38–43
- **Código:**
  ```java
  String senhaPreview = adminPassword != null && adminPassword.length() >= 3
          ? adminPassword.substring(0, 3) + "***"
          : "(vazia ou nula)";
  System.out.println("[AdminInitializer] Email lido: " + adminEmail);
  System.out.println("[AdminInitializer] Senha começa com: " + senhaPreview);
  ```
- **Por que é explorável:** A cada inicialização da aplicação (deploys, restarts no Render ou reinicializações locais), os logs do container gravam o e-mail exato do administrador e o prefixo inicial da senha real de produção. Logs em plataformas de nuvem são frequentemente acessíveis a múltiplos desenvolvedores, mantidos em históricos de deploy ou integrados a provedores de log externos (Datadog, Papertrail), expondo credenciais e reduzindo a entropia da senha para ataques offline.
- **Severidade:** **ALTA** (CWE-532: Insertion of Sensitive Information into Log File).

---

### Achado 3: Credenciais Padrão Conhecidas e E-mail de Super Admin com Fallback Inseguro
- **Arquivos:**
  - `backend/src/main/java/com/fichadigital/config/AdminInitializer.java` (Linhas 24–25)
  - `backend/src/test/java/karate-config.js` (Linhas 13–14)
  - `.env.example` (Linhas 11–12)
- **Código:**
  ```java
  @Value("${ADMIN_EMAIL:admin@fichadigital.com}")
  private String adminEmail;
  ```
  ```javascript
  adminEmail: 'admin@suafarmacia.com',
  adminPassword: 'SenhaForteDe16CaracteresOuMais!'
  ```
- **Por que é explorável:** Enquanto a senha `ADMIN_PASSWORD` exige obrigatoriedade no Spring Boot (fail-fast), o e-mail possui fallback público `admin@fichadigital.com`. Caso o ambiente de produção esqueça de configurar `ADMIN_EMAIL`, um invasor já sabe o alvo principal. Além disso, a senha documentada em `.env.example` é copiada diretamente no pipeline de CI (`qa-pipeline.yml:31`), tornando-se senha padrão ativa sempre que alguém subir ambientes a partir do template de exemplo.
- **Severidade:** **MÉDIA** (CWE-1188 / CWE-798).

---

### Achado 4: Ausência de Validação de Input (`@Valid`, `@Size`) em DTOs de Escrita
- **Arquivos:**
  - `backend/src/main/java/com/fichadigital/cliente/ClienteController.java` (Linhas 91–97)
  - `backend/src/main/java/com/fichadigital/cliente/AtualizarTelefoneRequest.java` (Linha 7)
  - `backend/src/main/java/com/fichadigital/lancamento/LancarFiadoRequest.java` (Linhas 17 e 20)
- **Código:**
  ```java
  // ClienteController.java: falta @Valid
  @PatchMapping("/{id}/telefone")
  public ResponseEntity<ClienteDetalhe> atualizarTelefone(
          @PathVariable UUID id,
          @RequestBody AtualizarTelefoneRequest request) { ... }
  ```
  ```java
  // LancarFiadoRequest.java: campos sem anotação de tamanho máximo
  String descricao,
  String pessoaRetirou
  ```
- **Por que é explorável:** O endpoint `PATCH /clientes/{id}/telefone` não anota o `@RequestBody` com `@Valid`, e o record `AtualizarTelefoneRequest` não possui constraints de tamanho. Como a coluna `telefone` no banco é `VARCHAR(20)`, o envio de uma string com mais de 20 caracteres causa uma exceção de banco de dados (`DataException`), retornando erro 500 desnecessário. Similarmente, `descricao` e `pessoaRetirou` em lançamentos de fiado aceitam payloads arbitrariamente gigantescos, gerando potencial degradação de desempenho ou preenchimento malicioso de armazenamento.
- **Severidade:** **MÉDIA** (CWE-20: Improper Input Validation).

---

### Achado 5: Ausência de Trava de Auto-Operação e Auto-Exclusão no Painel Super Admin
- **Arquivo:** `backend/src/main/java/com/fichadigital/admin/AdminService.java`
- **Linhas:** 94–115
- **Código:**
  ```java
  @Transactional
  public AdminDtos.UsuarioAdminResponse alterarStatus(UUID usuarioId, boolean ativo) {
      Usuario usuario = usuarioRepository.findById(usuarioId)
              .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado: " + usuarioId));
      usuario.setAtivo(ativo);
      return toResponse(usuarioRepository.save(usuario));
  }

  @Transactional
  public void excluirUsuario(UUID usuarioId) {
      if (!usuarioRepository.existsById(usuarioId)) {
          throw new EntityNotFoundException("Usuário não encontrado: " + usuarioId));
      }
      usuarioRepository.deleteById(usuarioId);
  }
  ```
- **Por que é explorável:** Não há validação impedindo que o Super Admin autenticado desative ou exclua sua própria conta (`usuarioId == SecurityUtils.usuarioId()`) ou exclua o Super Admin raiz gerado na inicialização. Em caso de envio acidental de requisição ou CSRF/comprometimento de sessão, o sistema pode entrar em estado de *lockout* permanente de administração global sem possibilidade de recuperação sem intervenção manual direta no banco de dados.
- **Severidade:** **BAIXA** (CWE-284: Improper Access Control / Resiliência Operacional).

---

### Achado 6: Divergência de Autorização entre Frontend (`roleGuard`) e Backend para Perfil `SUPER_ADMIN`
- **Arquivos:**
  - `frontend/src/app/core/auth/role.guard.ts` (Linha 18)
  - `backend/src/main/java/com/fichadigital/farmacia/FarmaciaController.java` (Linha 45)
- **Código:**
  ```typescript
  // role.guard.ts
  if (perfil === 'SUPER_ADMIN') return true;
  ```
  ```java
  // FarmaciaController.java
  @GetMapping("/resumo")
  @PreAuthorize("hasRole('DONO')")
  public ResponseEntity<ResumoFarmaciaResponse> resumo() { ... }
  ```
- **Por que é explorável:** O guard de rotas do Angular presume que o perfil `SUPER_ADMIN` tem permissão para navegar irrestritamente em todas as telas (`/dashboard`, etc.). No entanto, o backend exige rigorosamente `hasRole('DONO')` em `/farmacias/resumo` e depende de `SecurityUtils.farmaciaId()`. Como o Super Admin não tem farmácia associada (`farmacia == null`), a requisição do Dashboard falha com erro 403 Forbidden no Spring Security e a chamada a `farmaciaId()` lançaria `NullPointerException`. A política de autorização deve ser simétrica entre cliente e servidor.
- **Severidade:** **INFORMATIVA** (CWE-639 / Inconsistência de Regras de Negócio).

---

## 6. Recomendações Priorizadas

### Prioridade 1 (P1 — Imediata / Próximo Commit)
1. **Corrigir Rate Limiting de Login:**
   - Adicionar limite duplo no Bucket4j: por **e-mail do usuário alvo** (chave principal) e por **IP de origem verificado**.
   - Configurar o Spring Boot para processar cabeçalhos de proxy reverso apenas quando configurado confiavelmente (`server.forward-headers-strategy: framework` ou ler diretamente `getRemoteAddr()`).
2. **Remover Log de Senha do Super Admin:**
   - Remover as linhas 38 a 43 de `AdminInitializer.java`. Nunca emitir qualquer fragmento de senha em `System.out.println` ou frameworks de log.

### Prioridade 2 (P2 — Curto Prazo)
3. **Adicionar Validação nos DTOs de Escrita:**
   - Incluir `@Valid` em `ClienteController.atualizarTelefone`.
   - Adicionar `@Pattern` e `@Size(max = 20)` em `AtualizarTelefoneRequest`.
   - Adicionar `@Size(max = 255)` em `LancarFiadoRequest.descricao` e `pessoaRetirou`.
4. **Remover Fallback de E-mail de Administrador:**
   - Tornar `${ADMIN_EMAIL}` estritamente obrigatório em `AdminInitializer.java`, sem valor padrão embutido.

### Prioridade 3 (P3 — Médio Prazo)
5. **Implementar Trava de Auto-Operação em Usuários Administrativos:**
   - No `AdminService`, verificar se `usuarioId.equals(SecurityUtils.usuarioId())` antes de desativar ou excluir usuário, retornando `IllegalArgumentException("Não é permitido desativar ou excluir a si próprio")`.
6. **Alinhamento do `roleGuard` no Frontend:**
   - Em `role.guard.ts`, redirecionar `SUPER_ADMIN` para `/admin` em vez de permitir entrada em rotas exclusivas de donos de farmácia (`/dashboard`).

### Prioridade 4 (P4 — Melhoria de Arquitetura / Longo Prazo)
7. **Defesa em Profundidade com Row-Level Security (RLS) no PostgreSQL:**
   - Habilitar RLS nas tabelas `cliente`, `lancamento` e `pagamento` com política associada ao usuário ou sessão de banco, garantindo que mesmo se uma query JPA for escrita incorretamente no futuro, o PostgreSQL rejeite vazamentos inter-tenant.

---

## 7. ISSUES PARA O GITHUB (Prontas para Copiar e Colar)

--- ISSUE 1 ---
### [Segurança] Corrigir bypass de rate limiting no login via spoofing de X-Forwarded-For

**Labels:** `security`, `severity:high`, `auth`

#### Descrição do Problema
O método `AuthService.getClientIp()` consome cegamente o cabeçalho HTTP `X-Forwarded-For` enviado pelo cliente. Além disso, o rate limiting do Bucket4j é indexado exclusivamente pelo IP retornado, em vez de aplicar bloqueio pelo e-mail do usuário que está tentando se autenticar. Um atacante pode alternar o cabeçalho `X-Forwarded-For` em cada tentativa de login e realizar ataques de força bruta ilimitados contra qualquer conta.

#### Evidência
- Arquivo: `backend/src/main/java/com/fichadigital/auth/AuthService.java`
- Linhas: 50–55 e 73–77
```java
private String getClientIp() {
    String xfHeader = httpServletRequest.getHeader("X-Forwarded-For");
    if (xfHeader == null || xfHeader.isEmpty()) {
        return httpServletRequest.getRemoteAddr();
    }
    return xfHeader.split(",")[0].trim();
}
```

#### Impacto
Quebra total da proteção contra força bruta e preenchimento de credenciais (*credential stuffing*) em contas privilegiadas (DONO e SUPER_ADMIN).

#### Sugestão de Correção
1. Aplicar rate limiting composto indexado pelo e-mail do usuário (`email.toLowerCase()`).
2. Obter o IP remoto de forma segura sem confiar em cabeçalhos forjáveis a menos que a aplicação esteja atrás de um proxy reverso explicitamente configurado no Spring (`server.forward-headers-strategy: framework`).

#### Critérios de Aceite
- [ ] O Bucket4j aplica limite de 5 tentativas por 5 minutos por e-mail de destino.
- [ ] Enviar requisições repetidas com `X-Forwarded-For` forjado para o mesmo e-mail resulta em HTTP 429 após 5 tentativas.
- [ ] Testes automatizados cobrem a tentativa de bypass com múltiplos IPs forjados.
--- FIM ISSUE 1 ---

--- ISSUE 2 ---
### [Segurança] Remover vazamento de prefixo de senha do Super Admin no log/stdout

**Labels:** `security`, `severity:high`, `secrets`

#### Descrição do Problema
O componente `AdminInitializer` imprime no `stdout` o e-mail do Super Admin e os três primeiros caracteres da senha real lida das variáveis de ambiente de produção durante o startup da aplicação.

#### Evidência
- Arquivo: `backend/src/main/java/com/fichadigital/config/AdminInitializer.java`
- Linhas: 38–43
```java
String senhaPreview = adminPassword != null && adminPassword.length() >= 3
        ? adminPassword.substring(0, 3) + "***"
        : "(vazia ou nula)";
System.out.println("[AdminInitializer] Email lido: " + adminEmail);
System.out.println("[AdminInitializer] Senha começa com: " + senhaPreview);
```

#### Impacto
Vazamento de material de credencial em logs de container e plataformas de hospedagem (Render, CloudWatch), facilitando ataques de força bruta offline e violando o CWE-532.

#### Sugestão de Correção
Remover completamente os `System.out.println` que exibem detalhes do e-mail e da senha. Registrar apenas uma mensagem genérica via logger configurado: `log.info("Inicialização do Super Admin concluída com sucesso.");`.

#### Critérios de Aceite
- [ ] Nenhuma informação sobre a senha (nem mesmo prefixos ou máscaras) é emitida no console ou logs.
- [ ] `System.out.println` substituído por logger SLF4J padrão.
- [ ] O startup da aplicação em produção não gera saídas com dados sensíveis.
--- FIM ISSUE 2 ---

--- ISSUE 3 ---
### [Segurança] Tornar ADMIN_EMAIL estritamente obrigatório sem fallback público

**Labels:** `security`, `severity:medium`, `configuration`

#### Descrição do Problema
O `AdminInitializer` define um valor padrão embutido `admin@fichadigital.com` caso a variável de ambiente `ADMIN_EMAIL` não seja fornecida. Enquanto a senha possui fail-fast, o e-mail possui um valor público previsível.

#### Evidência
- Arquivo: `backend/src/main/java/com/fichadigital/config/AdminInitializer.java`
- Linhas: 24–25
```java
@Value("${ADMIN_EMAIL:admin@fichadigital.com}")
private String adminEmail;
```

#### Impacto
Ambientes mal configurados podem criar um Super Admin com identificador conhecido publicamente, facilitando enumeração e ataques direcionados.

#### Sugestão de Correção
Alterar `@Value("${ADMIN_EMAIL:admin@fichadigital.com}")` para `@Value("${ADMIN_EMAIL}")`, forçando o Spring Boot a interromper a inicialização caso a variável não seja definida.

#### Critérios de Aceite
- [ ] A aplicação falha no boot se `ADMIN_EMAIL` não for configurada.
- [ ] O fallback hardcoded é completamente removido.
- [ ] O arquivo `.env.example` e documentações continuam apontando a necessidade de definição da variável.
--- FIM ISSUE 3 ---

--- ISSUE 4 ---
### [Segurança] Adicionar @Valid e limites de tamanho em campos de clientes e lançamentos

**Labels:** `security`, `severity:medium`, `validation`

#### Descrição do Problema
O endpoint `PATCH /clientes/{id}/telefone` recebe o corpo da requisição sem a anotação `@Valid` e o DTO `AtualizarTelefoneRequest` não possui validação de formato ou tamanho. Adicionalmente, campos de texto livre em `LancarFiadoRequest` (`descricao` e `pessoaRetirou`) não possuem restrição de tamanho máximo.

#### Evidência
- Arquivo: `backend/src/main/java/com/fichadigital/cliente/ClienteController.java` (Linha 91)
- Arquivo: `backend/src/main/java/com/fichadigital/cliente/AtualizarTelefoneRequest.java` (Linha 7)
- Arquivo: `backend/src/main/java/com/fichadigital/lancamento/LancarFiadoRequest.java` (Linhas 17 e 20)
```java
@PatchMapping("/{id}/telefone")
public ResponseEntity<ClienteDetalhe> atualizarTelefone(
        @PathVariable UUID id,
        @RequestBody AtualizarTelefoneRequest request) { ... }
```

#### Impacto
- Envio de telefone com mais de 20 caracteres estoura `DataException` no PostgreSQL gerando HTTP 500 em vez de 400 Bad Request.
- Possibilidade de envio de cargas massivas em `descricao`, causando consumo excessivo de memória ou espaço em disco.

#### Sugestão de Correção
1. Adicionar `@Valid` no `@RequestBody` de `atualizarTelefone`.
2. Adicionar `@Size(max = 20)` em `AtualizarTelefoneRequest.telefone`.
3. Adicionar `@Size(max = 255)` em `LancarFiadoRequest.descricao` e `pessoaRetirou`.

#### Critérios de Aceite
- [ ] Requisições com telefone > 20 caracteres retornam HTTP 400 com detalhe de validação RFC 9457.
- [ ] Campos de descrição e pessoaRetirou rejeitam strings superiores a 255 caracteres com HTTP 400.
--- FIM ISSUE 4 ---

--- ISSUE 5 ---
### [Segurança] Implementar trava de proteção contra auto-desativação e auto-exclusão no Admin

**Labels:** `security`, `severity:low`, `admin`

#### Descrição do Problema
Os endpoints de gerenciamento de usuários em `AdminService` (`alterarStatus` e `excluirUsuario`) permitem que qualquer Super Admin altere o status ou delete o seu próprio usuário (`usuarioId == usuarioAutenticado.getId()`), ou a conta principal criada pelo inicializador.

#### Evidência
- Arquivo: `backend/src/main/java/com/fichadigital/admin/AdminService.java`
- Linhas: 101–115
```java
@Transactional
public AdminDtos.UsuarioAdminResponse alterarStatus(UUID usuarioId, boolean ativo) { ... }

@Transactional
public void excluirUsuario(UUID usuarioId) { ... }
```

#### Impacto
Risco de lockout permanente da administração do sistema por erro operacional ou manipulação acidental de requisição.

#### Sugestão de Correção
Validar se `usuarioId.equals(SecurityUtils.usuarioId())` e lançar `IllegalArgumentException("Não é permitido alterar o status ou excluir o próprio usuário logado.")`.

#### Critérios de Aceite
- [ ] Tentativa de auto-desativação do usuário logado retorna HTTP 409 Conflict com mensagem explicativa.
- [ ] Tentativa de auto-exclusão do usuário logado é bloqueada.
- [ ] Outros usuários continuam podendo ser ativados/desativados normalmente.
--- FIM ISSUE 5 ---
