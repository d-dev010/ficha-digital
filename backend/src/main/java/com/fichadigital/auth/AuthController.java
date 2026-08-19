package com.fichadigital.auth;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller de autenticação — rota pública (sem JWT).
 * POST /auth/login — US02
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /auth/login
     * Autentica um usuário (dono ou atendente) e retorna o JWT.
     *
     * @param request email + senha
     * @return 200 OK com { token, usuarioId, nome, perfil, farmaciaId }
     *         400 Bad Request se body inválido
     *         401 Unauthorized se credenciais erradas
     */
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.autenticar(request));
    }
}
