package com.fichadigital.admin;

import com.fichadigital.usuario.Perfil;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** DTOs usados pelo AdminController. */
public final class AdminDtos {

    private AdminDtos() {}

    // ─── Farmácias ────────────────────────────────────────────────

    public record FarmaciaAdminResponse(
            UUID id,
            String nome,
            String cnpj,
            long totalUsuarios
    ) {}

    public record CriarFarmaciaAdminRequest(
            @NotBlank String nomeFarmacia,
            String cnpj,
            @NotBlank String nomeDono,
            @Email @NotBlank String emailDono,
            @NotBlank @Size(min = 8) String senhaDono
    ) {}

    // ─── Usuários ─────────────────────────────────────────────────

    public record UsuarioAdminResponse(
            UUID id,
            String nome,
            String email,
            Perfil perfil,
            boolean ativo,
            UUID farmaciaId,
            String farmaciaNome
    ) {}

    public record CriarUsuarioAdminRequest(
            @NotBlank String nome,
            @Email @NotBlank String email,
            @NotBlank @Size(min = 8) String senha,
            @NotNull Perfil perfil,
            @NotNull UUID farmaciaId
    ) {}

    public record AlterarSenhaAdminRequest(
            @NotBlank @Size(min = 8) String novaSenha
    ) {}

    public record AlterarStatusRequest(
            boolean ativo
    ) {}
}
