package com.fichadigital.admin;

import com.fichadigital.admin.AdminDtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Endpoints exclusivos do SUPER_ADMIN.
 * Todas as rotas exigem perfil SUPER_ADMIN via @PreAuthorize.
 * Prefixo: /admin
 */
@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // ─── Farmácias ────────────────────────────────────────────────

    @GetMapping("/farmacias")
    public List<FarmaciaAdminResponse> listarFarmacias() {
        return adminService.listarFarmacias();
    }

    @PostMapping("/farmacias")
    @ResponseStatus(HttpStatus.CREATED)
    public FarmaciaAdminResponse criarFarmacia(@Valid @RequestBody CriarFarmaciaAdminRequest req) {
        return adminService.criarFarmacia(req);
    }

    @DeleteMapping("/farmacias/{farmaciaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirFarmacia(@PathVariable UUID farmaciaId) {
        adminService.excluirFarmacia(farmaciaId);
    }

    // ─── Usuários ─────────────────────────────────────────────────

    /** Lista usuários. Filtra por farmácia se o parâmetro farmaciaId for passado. */
    @GetMapping("/usuarios")
    public List<UsuarioAdminResponse> listarUsuarios(
            @RequestParam(required = false) UUID farmaciaId) {
        return adminService.listarUsuarios(farmaciaId);
    }

    @PostMapping("/usuarios")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioAdminResponse criarUsuario(@Valid @RequestBody CriarUsuarioAdminRequest req) {
        return adminService.criarUsuario(req);
    }

    @PatchMapping("/usuarios/{usuarioId}/senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void alterarSenha(
            @PathVariable UUID usuarioId,
            @Valid @RequestBody AlterarSenhaAdminRequest req) {
        adminService.alterarSenha(usuarioId, req.novaSenha());
    }

    @PatchMapping("/usuarios/{usuarioId}/status")
    public UsuarioAdminResponse alterarStatus(
            @PathVariable UUID usuarioId,
            @Valid @RequestBody AlterarStatusRequest req) {
        return adminService.alterarStatus(usuarioId, req.ativo());
    }

    @DeleteMapping("/usuarios/{usuarioId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirUsuario(@PathVariable UUID usuarioId) {
        adminService.excluirUsuario(usuarioId);
    }
}
