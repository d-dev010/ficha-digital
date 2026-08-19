package com.fichadigital.cliente;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO para POST /clientes — cadastro de cliente (US03).
 */
public record CadastrarClienteRequest(
        @NotBlank(message = "Nome é obrigatório")
        String nome,

        String telefone,

        /** CPF é opcional conforme US03. */
        String cpf
) {}
