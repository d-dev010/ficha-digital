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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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

    // Cache para evitar buscar no banco de dados a cada requisição (problema de performance do JWT stateful).
    // TTL de 5 minutos permite revogação quase imediata sem comprometer o banco de dados.
    private final Cache<UUID, Boolean> activeUsersCache = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build();

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

        Boolean isActive = activeUsersCache.get(usuarioId, id -> {
            return usuarioRepository.findById(id)
                    .map(u -> {
                        // Salva os UserDetails no cache? Não, apenas se está ativo, 
                        // pois UserDetails tem entidades amarradas que podem dar lazy load exception.
                        // Para o SecurityContext, precisamos do UserDetails.
                        // Para contornar e continuar simples:
                        return u.isEnabled();
                    }).orElse(false);
        });

        if (Boolean.FALSE.equals(isActive)) {
            log.warn("Tentativa de acesso com token válido por usuário desativado ou inexistente (id={})", usuarioId);
            filterChain.doFilter(request, response);
            return;
        }

        // Como o usuário está ativo (verificado pelo cache em memória), 
        // não precisamos ir ao banco de dados para criar o UserDetails.
        // O próprio Token tem tudo que precisamos (userId, perfil, farmaciaId).
        UUID farmaciaId = jwtService.extrairFarmaciaId(token);
        String perfilStr = jwtService.extrairPerfil(token);

        com.fichadigital.usuario.Usuario usuario = new com.fichadigital.usuario.Usuario();
        usuario.setId(usuarioId);
        usuario.setPerfil(com.fichadigital.usuario.Perfil.valueOf(perfilStr));
        if (farmaciaId != null) {
            com.fichadigital.farmacia.Farmacia farmacia = new com.fichadigital.farmacia.Farmacia();
            farmacia.setId(farmaciaId);
            usuario.setFarmacia(farmacia);
        }
        usuario.setAtivo(true);
        // Não precisamos de senha/email no SecurityContext para as rotas da API

        org.springframework.security.core.userdetails.UserDetails userDetails = usuario;
        org.springframework.security.authentication.UsernamePasswordAuthenticationToken authToken =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        authToken.setDetails(new org.springframework.security.web.authentication.WebAuthenticationDetailsSource().buildDetails(request));
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(authToken);

        filterChain.doFilter(request, response);
    }
}
