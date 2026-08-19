package com.fichadigital.cliente;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO de detalhe para GET /clientes/{id} — CPF completo (RNF04: só no detalhe).
 */
public record ClienteDetalhe(
        UUID id,
        String nome,
        String telefone,
        String cpf,
        BigDecimal saldoDevedor
) {
    public static ClienteDetalhe from(Cliente c) {
        return new ClienteDetalhe(c.getId(), c.getNome(), c.getTelefone(), c.getCpf(), c.getSaldoDevedor());
    }
}
