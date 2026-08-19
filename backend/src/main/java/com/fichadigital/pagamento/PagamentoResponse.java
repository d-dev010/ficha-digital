package com.fichadigital.pagamento;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO de response para pagamentos.
 */
public record PagamentoResponse(
        UUID id,
        BigDecimal valor,
        Instant data,
        UUID usuarioId,
        String nomeUsuario
) {
    public static PagamentoResponse from(Pagamento p) {
        return new PagamentoResponse(
                p.getId(), p.getValor(), p.getData(),
                p.getUsuario().getId(), p.getUsuario().getNome()
        );
    }
}
