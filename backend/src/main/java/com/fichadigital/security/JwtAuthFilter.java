package com.fichadigital.security;

import com.fichadigital.usuario.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filtro JWT que intercepta cada request e popula o SecurityContext.
 *
 * Ao popular o SecurityContext, o farmaciaId extraído do JWT fica disponível
 * para qualquer Service através de SecurityContextHolder — nunca do body (RNF03).
 *
 * Fluxo:
 *  1. Extrai "Bearer <token>" do header Authorization
 *  2. Valida o token (assinatura + expiração)
 *  3. Carrega o Usuario do banco
 *  4. Seta autenticação no SecurityContext
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(7);

        if (!jwtService.validarToken(token)) {
            log.warn("Token JWT inválido ou expirado para request: {} {}", request.getMethod(), request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        // Já autenticado na mesma request — não reprocessar
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        UUID usuarioId = jwtService.extrairUsuarioId(token);

        usuarioRepository.findById(usuarioId).ifPresent(usuario -> {
            // Problema 5 — Segurança: recusa autenticação se o usuário foi desativado.
            // Isso revoga sessões instantaneamente mesmo que o JWT ainda não tenha expirado.
            if (!usuario.isEnabled()) {
                log.warn("Tentativa de acesso com token válido por usuário desativado (id={})", usuarioId);
                return;
            }
            UserDetails userDetails = usuario;
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
        });

        filterChain.doFilter(request, response);
    }
}
