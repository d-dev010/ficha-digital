package com.fichadigital.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO para POST /usuarios — cadastro de funcionário pelo DONO (US01).
 */
public record CadastrarFuncionarioRequest(
        @NotBlank(message = "Nome é obrigatório")
        String nome,

        @Email(message = "E-mail inválido")
        @NotBlank(message = "E-mail é obrigatório")
        String email,

        @NotBlank(message = "Senha temporária é obrigatória")
        @Size(min = 8, message = "Senha deve ter ao menos 8 caracteres")
        String senhaTemporaria
) {}
