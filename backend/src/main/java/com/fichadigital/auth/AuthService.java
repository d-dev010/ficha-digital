package com.fichadigital.auth;

import com.fichadigital.security.JwtService;
import com.fichadigital.usuario.Usuario;
import com.fichadigital.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

/**
 * Service de autenticação.
 * Delega validação de credenciais ao AuthenticationManager do Spring Security,
 * que por sua vez usa BCrypt via DaoAuthenticationProvider (RNF01).
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;

    /**
     * Autentica o usuário e retorna um JWT.
     *
     * @param request DTO com email e senha em texto puro (recebido pela HTTPS).
     * @return TokenResponse com o JWT e dados básicos do usuário.
     * @throws AuthenticationException se credenciais inválidas.
     */
    public TokenResponse autenticar(LoginRequest request) {
        // Spring Security valida email + bcrypt(senha) automaticamente
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.senha())
        );

        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .orElseThrow(); // nunca ocorre aqui: authenticate() já validou

        String token = jwtService.gerarToken(usuario);

        return new TokenResponse(
                token,
                usuario.getId(),
                usuario.getNome(),
                usuario.getPerfil(),
                usuario.getFarmacia().getId()
        );
    }
}
