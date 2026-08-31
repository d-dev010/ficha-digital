package com.fichadigital.lancamento;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO de response para lançamentos.
 */
public record LancamentoResponse(
        UUID id,
        BigDecimal valor,
        String descricao,
        Instant data,
        UUID usuarioId,
        String nomeUsuario,
        String pessoaRetirou
) {
    public static LancamentoResponse from(Lancamento l) {
        return new LancamentoResponse(
                l.getId(), l.getValor(), l.getDescricao(), l.getData(),
                l.getUsuario().getId(), l.getUsuario().getNome(),
                l.getPessoaRetirou()
        );
    }
}
