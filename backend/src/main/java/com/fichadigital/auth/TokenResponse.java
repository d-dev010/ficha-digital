package com.fichadigital.auth;

import com.fichadigital.usuario.Perfil;

import java.util.UUID;

/**
 * DTO de response para POST /auth/login.
 * Retorna o token JWT + informações não-sensíveis do usuário.
 */
public record TokenResponse(
        String token,
        UUID usuarioId,
        String nome,
        Perfil perfil,
        UUID farmaciaId
) {}
