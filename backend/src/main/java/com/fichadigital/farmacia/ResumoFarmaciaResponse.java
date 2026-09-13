package com.fichadigital.farmacia;

import java.math.BigDecimal;

/**
 * DTO de resposta do endpoint GET /farmacias/resumo.
 * Expõe métricas do painel administrativo do DONO.
 *
 * @param clientesAtivos  Número de clientes com saldo devedor > 0
 * @param totalAReceber   Soma de todos os saldos devedores da farmácia
 */
public record ResumoFarmaciaResponse(long clientesAtivos, BigDecimal totalAReceber) {}
