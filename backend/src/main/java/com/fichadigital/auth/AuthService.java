package com.fichadigital.auth;

import com.fichadigital.security.JwtService;
import com.fichadigital.usuario.Usuario;
import com.fichadigital.usuario.UsuarioRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

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
    private final HttpServletRequest httpServletRequest;

    /**
     * Cache de buckets de Rate Limiting por e-mail com TTL de 1 hora.
     *
     * Troca o ConcurrentHashMap sem expiração por Caffeine com eviction automática:
     * e-mails inativos por 1h são removidos da memória, prevenindo DoS por esgotamento de RAM
     * (um atacante não pode mais encher o mapa com infinitos e-mails aleatórios).
     */
    private final Cache<String, Bucket> loginBuckets = Caffeine.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .maximumSize(100_000) // teto de segurança adicional
            .build();

    private String getClientIp() {
        String xfHeader = httpServletRequest.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return httpServletRequest.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }

    private Bucket resolveBucket(String key) {
        return loginBuckets.get(email, k -> {
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
        String ip = getClientIp();
        Bucket bucket = resolveBucket(ip);
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

        // SUPER_ADMIN não tem farmácia — farmaciaId será null no token
        java.util.UUID farmaciaId = usuario.getFarmacia() != null
                ? usuario.getFarmacia().getId()
                : null;

        return new TokenResponse(
                token,
                usuario.getId(),
                usuario.getNome(),
                usuario.getPerfil(),
                farmaciaId
        );
    }
}
