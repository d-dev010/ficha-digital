package com.fichadigital.security;

import com.fichadigital.usuario.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * Serviço de geração e validação de JWT.
 *
 * Claims do token:
 *  - sub       : userId (UUID)
 *  - farmaciaId: UUID da farmácia (RNF03 — nunca vem do request body)
 *  - perfil    : DONO ou ATENDENTE
 *  - exp       : 8 horas (RNF02)
 *
 * A extração de farmaciaId SEMPRE vem deste token — nunca do payload da request.
 */
@Service
public class JwtService {

    private static final String CLAIM_FARMACIA_ID = "farmaciaId";
    private static final String CLAIM_PERFIL = "perfil";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        // Garante 256 bits mínimos para HMAC-SHA256
        if (keyBytes.length < 32) {
            throw new IllegalStateException("jwt.secret deve ter ao menos 32 caracteres (256 bits)");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Gera um JWT para o usuário autenticado.
     *
     * @param usuario Entidade do usuário autenticado com farmacia já carregada.
     * @return Token JWT assinado.
     */
    public String gerarToken(Usuario usuario) {
        Date agora = new Date();
        Date expiracao = new Date(agora.getTime() + expirationMs);

        return Jwts.builder()
                .subject(usuario.getId().toString())
                .claim(CLAIM_FARMACIA_ID, usuario.getFarmacia().getId().toString())
                .claim(CLAIM_PERFIL, usuario.getPerfil().name())
                .issuedAt(agora)
                .expiration(expiracao)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extrai o userId do token.
     */
    public UUID extrairUsuarioId(String token) {
        return UUID.fromString(extrairClaims(token).getSubject());
    }

    /**
     * Extrai o farmaciaId do token (RNF03).
     * SEMPRE deve ser usado para filtrar queries — nunca o valor do body.
     */
    public UUID extrairFarmaciaId(String token) {
        String farmaciaIdStr = extrairClaims(token).get(CLAIM_FARMACIA_ID, String.class);
        return UUID.fromString(farmaciaIdStr);
    }

    /**
     * Extrai o perfil (DONO/ATENDENTE) do token.
     */
    public String extrairPerfil(String token) {
        return extrairClaims(token).get(CLAIM_PERFIL, String.class);
    }

    /**
     * Valida assinatura e expiração do token.
     *
     * @return true se o token for válido e não expirado.
     */
    public boolean validarToken(String token) {
        try {
            extrairClaims(token); // lança exceção se inválido ou expirado
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims extrairClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
