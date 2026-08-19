package com.fichadigital.pagamento;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * DTO para POST /clientes/{id}/pagamentos — registrar pagamento (US07).
 * Nota: clienteId e farmaciaId NÃO estão neste DTO — vêm do path e do JWT respectivamente (RNF03).
 */
public record RegistrarPagamentoRequest(
        @NotNull(message = "Valor é obrigatório")
        @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
        BigDecimal valor
) {}
