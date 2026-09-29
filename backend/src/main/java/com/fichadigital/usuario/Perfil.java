package com.fichadigital.usuario;

/**
 * Perfis de acesso do sistema.
 * SUPER_ADMIN — visão global de todas as farmácias; sem farmacia_id.
 * DONO        — acesso total dentro da sua farmácia (relatórios, exclusões).
 * ATENDENTE   — lança e consulta fichas; sem acesso a relatórios e exclusões.
 */
public enum Perfil {
    SUPER_ADMIN,
    DONO,
    ATENDENTE
}
