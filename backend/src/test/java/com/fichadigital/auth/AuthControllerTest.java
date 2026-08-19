package com.fichadigital.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fichadigital.usuario.Perfil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração para AuthController (camada web + security).
 * Usa @WebMvcTest para testar apenas a camada HTTP sem subir contexto completo.
 */
@WebMvcTest(AuthController.class)
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    @DisplayName("POST /auth/login deve retornar 400 quando body está vazio")
    void deveRetornar400ParaBodyVazio() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /auth/login deve retornar 400 quando e-mail é inválido")
    void deveRetornar400ParaEmailInvalido() throws Exception {
        LoginRequest request = new LoginRequest("email-invalido", "senha123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /auth/login deve retornar 401 quando credenciais são inválidas")
    void deveRetornar401ParaCredenciaisInvalidas() throws Exception {
        LoginRequest request = new LoginRequest("usuario@farmacia.com", "senhaErrada");
        when(authService.autenticar(any())).thenThrow(new BadCredentialsException("Credenciais inválidas"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /auth/login deve retornar 200 com token quando credenciais são válidas")
    void deveRetornar200ComTokenParaCredenciaisValidas() throws Exception {
        LoginRequest request = new LoginRequest("dono@farmacia.com", "senha123");
        TokenResponse tokenResponse = new TokenResponse(
                "jwt.token.aqui",
                UUID.randomUUID(),
                "Dono da Farmácia",
                Perfil.DONO,
                UUID.randomUUID()
        );
        when(authService.autenticar(any())).thenReturn(tokenResponse);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt.token.aqui"))
                .andExpect(jsonPath("$.perfil").value("DONO"));
    }
}
