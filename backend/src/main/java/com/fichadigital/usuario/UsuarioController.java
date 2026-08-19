package com.fichadigital.usuario;

import com.fichadigital.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller de usuários.
 * POST /usuarios — US01 (dono cadastra funcionário)
 * Requer perfil DONO.
 */
@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    /**
     * POST /usuarios
     * Cadastra um funcionário (ATENDENTE) na farmácia do dono autenticado.
     * farmaciaId extraído do JWT via SecurityUtils (RNF03) — nunca do body.
     *
     * @return 201 Created com dados básicos do funcionário criado.
     */
    @PostMapping
    @PreAuthorize("hasRole('DONO')")
    public ResponseEntity<UsuarioResponse> cadastrarFuncionario(
            @Valid @RequestBody CadastrarFuncionarioRequest request) {

        // RNF03: farmaciaId SEMPRE do JWT
        UUID farmaciaId = SecurityUtils.farmaciaId();

        Usuario usuario = usuarioService.cadastrarFuncionario(farmaciaId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioResponse.from(usuario));
    }

    /**
     * DTO de response — nunca expõe senhaHash.
     */
    public record UsuarioResponse(UUID id, String nome, String email, Perfil perfil) {
        static UsuarioResponse from(Usuario u) {
            return new UsuarioResponse(u.getId(), u.getNome(), u.getEmail(), u.getPerfil());
        }
    }
}
