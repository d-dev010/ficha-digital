package com.fichadigital.usuario;

import com.fichadigital.farmacia.Farmacia;
import com.fichadigital.farmacia.FarmaciaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service de usuários.
 * Cadastro de funcionários (atendentes) — apenas DONO pode invocar (validado via @PreAuthorize no controller).
 * farmaciaId sempre vem do JWT/SecurityContext, nunca do body (RNF03).
 */
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final FarmaciaRepository farmaciaRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Cadastra um novo funcionário (ATENDENTE) na farmácia do dono autenticado.
     *
     * @param farmaciaId  UUID extraído do JWT — RNF03.
     * @param request     Dados do funcionário.
     * @return Usuario criado.
     */
    @Transactional
    public Usuario cadastrarFuncionario(UUID farmaciaId, CadastrarFuncionarioRequest request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("E-mail já cadastrado: " + request.email());
        }

        Farmacia farmacia = farmaciaRepository.findById(farmaciaId)
                .orElseThrow(() -> new EntityNotFoundException("Farmácia não encontrada"));

        Usuario funcionario = Usuario.builder()
                .farmacia(farmacia)
                .nome(request.nome())
                .email(request.email())
                .senhaHash(passwordEncoder.encode(request.senhaTemporaria())) // BCrypt — RNF01
                .perfil(Perfil.ATENDENTE)
                .build();

        return usuarioRepository.save(funcionario);
    }
}
