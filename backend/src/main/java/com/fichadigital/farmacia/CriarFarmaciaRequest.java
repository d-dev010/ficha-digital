package com.fichadigital.farmacia;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO de request para POST /farmacias.
 * Cria a farmácia e o usuário DONO em uma única chamada (US01).
 */
public record CriarFarmaciaRequest(
        @NotBlank(message = "Nome da farmácia é obrigatório")
        String nomeFarmacia,

        String cnpj,

        @NotBlank(message = "Nome do dono é obrigatório")
        String nomeDono,

        @Email(message = "E-mail inválido")
        @NotBlank(message = "E-mail do dono é obrigatório")
        String emailDono,

        @NotBlank(message = "Senha é obrigatória")
        @Size(min = 8, message = "Senha deve ter ao menos 8 caracteres")
        String senhaDono
) {}
