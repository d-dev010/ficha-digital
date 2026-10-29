package com.fichadigital.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fichadigital.security.JwtService;
import com.fichadigital.usuario.Perfil;
import com.fichadigital.usuario.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
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
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UsuarioRepository usuarioRepository;

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
        when(authService.autenticar(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("E-mail ou senha inválidos"));

        LoginRequest request = new LoginRequest("usuario@teste.com", "senha-errada");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /auth/login deve retornar 200 com TokenResponse quando sucesso")
    void deveRetornar200ComTokenResponseNoLoginComSucesso() throws Exception {
        UUID farmaciaId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        TokenResponse responseEsperada = new TokenResponse(
                "token-jwt-mock",
                usuarioId,
                "Dono Teste",
                Perfil.DONO,
                farmaciaId
        );

        when(authService.autenticar(any(LoginRequest.class))).thenReturn(responseEsperada);

        LoginRequest request = new LoginRequest("dono@teste.com", "SenhaForte123!");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token-jwt-mock"))
                .andExpect(jsonPath("$.usuarioId").value(usuarioId.toString()))
                .andExpect(jsonPath("$.nome").value("Dono Teste"))
                .andExpect(jsonPath("$.perfil").value("DONO"))
                .andExpect(jsonPath("$.farmaciaId").value(farmaciaId.toString()));
    }
}
