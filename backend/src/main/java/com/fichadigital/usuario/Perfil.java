package com.fichadigital.usuario;

/**
 * Perfis de acesso do sistema.
 * DONO — acesso total, incluindo relatórios e exclusões.
 * ATENDENTE — lança e consulta fichas; sem acesso a relatórios e exclusões.
 */
public enum Perfil {
    DONO,
    ATENDENTE
}
