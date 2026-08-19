package com.fichadigital.security;

import com.fichadigital.usuario.Usuario;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/**
 * Utilitário para extrair dados do usuário autenticado do SecurityContext.
 *
 * IMPORTANTE (RNF03): farmaciaId e usuarioId SEMPRE vêm daqui — nunca do corpo da requisição.
 * Este helper centraliza o acesso ao SecurityContext para que todos os controllers
 * obtenham esses valores de forma consistente.
 */
public final class SecurityUtils {

    private SecurityUtils() {}

    /**
     * Retorna o usuário autenticado no SecurityContext.
     */
    public static Usuario usuarioAutenticado() {
        return (Usuario) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    /**
     * Retorna o farmaciaId do usuário autenticado (RNF03).
     * Nunca use farmaciaId vindo do body/query param.
     */
    public static UUID farmaciaId() {
        return usuarioAutenticado().getFarmacia().getId();
    }

    /**
     * Retorna o usuarioId do usuário autenticado.
     */
    public static UUID usuarioId() {
        return usuarioAutenticado().getId();
    }
}
