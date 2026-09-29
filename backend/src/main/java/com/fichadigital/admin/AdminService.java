package com.fichadigital.admin;

import com.fichadigital.farmacia.Farmacia;
import com.fichadigital.farmacia.FarmaciaRepository;
import com.fichadigital.farmacia.FarmaciaService;
import com.fichadigital.farmacia.CriarFarmaciaRequest;
import com.fichadigital.usuario.Perfil;
import com.fichadigital.usuario.Usuario;
import com.fichadigital.usuario.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service do painel de administração global (SUPER_ADMIN).
 * Nenhum método filtra por farmaciaId — opera sobre todos os tenants.
 */
@Service
@RequiredArgsConstructor
public class AdminService {

    private final FarmaciaRepository farmaciaRepository;
    private final FarmaciaService farmaciaService;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    // ─── Farmácias ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<AdminDtos.FarmaciaAdminResponse> listarFarmacias() {
        return farmaciaRepository.findAll().stream()
                .map(f -> new AdminDtos.FarmaciaAdminResponse(
                        f.getId(),
                        f.getNome(),
                        f.getCnpj(),
                        usuarioRepository.countByFarmaciaId(f.getId())
                ))
                .toList();
    }

    @Transactional
    public AdminDtos.FarmaciaAdminResponse criarFarmacia(AdminDtos.CriarFarmaciaAdminRequest req) {
        Farmacia farmacia = farmaciaService.criarFarmaciaComDono(new CriarFarmaciaRequest(
                req.nomeFarmacia(), req.cnpj(), req.nomeDono(), req.emailDono(), req.senhaDono()
        ));
        long totalUsuarios = usuarioRepository.countByFarmaciaId(farmacia.getId());
        return new AdminDtos.FarmaciaAdminResponse(
                farmacia.getId(), farmacia.getNome(), farmacia.getCnpj(), totalUsuarios
        );
    }

    @Transactional
    public void excluirFarmacia(UUID farmaciaId) {
        if (!farmaciaRepository.existsById(farmaciaId)) {
            throw new EntityNotFoundException("Farmácia não encontrada: " + farmaciaId);
        }
        farmaciaRepository.deleteById(farmaciaId);
    }

    // ─── Usuários ─────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<AdminDtos.UsuarioAdminResponse> listarUsuarios(UUID farmaciaId) {
        List<Usuario> usuarios = (farmaciaId != null)
                ? usuarioRepository.findByFarmaciaId(farmaciaId)
                : usuarioRepository.findAllNonAdmin();
        return usuarios.stream().map(this::toResponse).toList();
    }

    @Transactional
    public AdminDtos.UsuarioAdminResponse criarUsuario(AdminDtos.CriarUsuarioAdminRequest req) {
        if (usuarioRepository.existsByEmail(req.email())) {
            throw new IllegalArgumentException("E-mail já cadastrado: " + req.email());
        }
        Farmacia farmacia = farmaciaRepository.findById(req.farmaciaId())
                .orElseThrow(() -> new EntityNotFoundException("Farmácia não encontrada"));

        Usuario usuario = Usuario.builder()
                .farmacia(farmacia)
                .nome(req.nome())
                .email(req.email())
                .senhaHash(passwordEncoder.encode(req.senha()))
                .perfil(req.perfil())
                .build();
        return toResponse(usuarioRepository.save(usuario));
    }

    @Transactional
    public void alterarSenha(UUID usuarioId, String novaSenha) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado: " + usuarioId));
        usuario.setSenhaHash(passwordEncoder.encode(novaSenha));
        usuarioRepository.save(usuario);
    }

    @Transactional
    public AdminDtos.UsuarioAdminResponse alterarStatus(UUID usuarioId, boolean ativo) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado: " + usuarioId));
        usuario.setAtivo(ativo);
        return toResponse(usuarioRepository.save(usuario));
    }

    @Transactional
    public void excluirUsuario(UUID usuarioId) {
        if (!usuarioRepository.existsById(usuarioId)) {
            throw new EntityNotFoundException("Usuário não encontrado: " + usuarioId);
        }
        usuarioRepository.deleteById(usuarioId);
    }

    // ─── Helpers ──────────────────────────────────────────────────

    private AdminDtos.UsuarioAdminResponse toResponse(Usuario u) {
        return new AdminDtos.UsuarioAdminResponse(
                u.getId(),
                u.getNome(),
                u.getEmail(),
                u.getPerfil(),
                u.isAtivo(),
                u.getFarmacia() != null ? u.getFarmacia().getId() : null,
                u.getFarmacia() != null ? u.getFarmacia().getNome() : null
        );
    }
}
