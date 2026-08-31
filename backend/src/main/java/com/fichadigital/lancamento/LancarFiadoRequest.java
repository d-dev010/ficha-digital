package com.fichadigital.lancamento;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * DTO para POST /clientes/{id}/lancamentos — lançar fiado (US05).
 * Nota: clienteId e farmaciaId NÃO estão neste DTO — vêm do path e do JWT respectivamente (RNF03).
 */
public record LancarFiadoRequest(
        @NotNull(message = "Valor é obrigatório")
        @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
        BigDecimal valor,

        String descricao,

        /** Nome de quem fisicamente retirou o item. Opcional — se nulo, assume-se o próprio cliente. */
        String pessoaRetirou
) {}
