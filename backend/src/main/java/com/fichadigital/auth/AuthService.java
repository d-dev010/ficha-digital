package com.fichadigital.auth;

import com.fichadigital.security.JwtService;
import com.fichadigital.usuario.Usuario;
import com.fichadigital.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Refill;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    // Cache simples de rate limiting por e-mail (Problema M3)
    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();

    private Bucket resolveBucket(String email) {
        return loginBuckets.computeIfAbsent(email, k -> {
            Refill refill = Refill.intervally(5, Duration.ofMinutes(5));
            Bandwidth limit = Bandwidth.classic(5, refill);
            return Bucket.builder().addLimit(limit).build();
        });
    }

    /**
     * Autentica o usuário e retorna um JWT.
     *
     * @param request DTO com email e senha em texto puro (recebido pela HTTPS).
     * @return TokenResponse com o JWT e dados básicos do usuário.
     * @throws AuthenticationException se credenciais inválidas.
     */
    public TokenResponse autenticar(LoginRequest request) {
        Bucket bucket = resolveBucket(request.email());
        if (!bucket.tryConsume(1)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Muitas tentativas de login. Tente novamente mais tarde.");
        }

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
