package com.fichadigital.cliente;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO de resumo para listagens — CPF mascarado (RNF04).
 */
public record ClienteResumo(
        UUID id,
        String nome,
        String telefone,
        String cpfMascarado,
        String endereco,
        BigDecimal saldoDevedor
) {
    public static ClienteResumo from(Cliente c, String cpfMascarado) {
        return new ClienteResumo(c.getId(), c.getNome(), c.getTelefone(), cpfMascarado, c.getEndereco(), c.getSaldoDevedor());
    }
}
