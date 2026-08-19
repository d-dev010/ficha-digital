package com.fichadigital.farmacia;

import com.fichadigital.usuario.Perfil;
import com.fichadigital.usuario.Usuario;
import com.fichadigital.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service de farmácias.
 * Operação principal: criar farmácia + usuário DONO em uma única transação (US01).
 */
@Service
@RequiredArgsConstructor
public class FarmaciaService {

    private final FarmaciaRepository farmaciaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Cria uma nova farmácia e seu usuário DONO em uma única transação atômica.
     * Senha é sempre hasheada com BCrypt antes de persistir (RNF01).
     *
     * @param request DTO com dados da farmácia e do dono.
     * @return Farmácia criada.
     * @throws IllegalArgumentException se CNPJ ou e-mail já existirem.
     */
    @Transactional
    public Farmacia criarFarmaciaComDono(CriarFarmaciaRequest request) {
        if (request.cnpj() != null && farmaciaRepository.existsByCnpj(request.cnpj())) {
            throw new IllegalArgumentException("CNPJ já cadastrado: " + request.cnpj());
        }
        if (usuarioRepository.existsByEmail(request.emailDono())) {
            throw new IllegalArgumentException("E-mail já cadastrado: " + request.emailDono());
        }

        Farmacia farmacia = Farmacia.builder()
                .nome(request.nomeFarmacia())
                .cnpj(request.cnpj())
                .build();
        farmacia = farmaciaRepository.save(farmacia);

        Usuario dono = Usuario.builder()
                .farmacia(farmacia)
                .nome(request.nomeDono())
                .email(request.emailDono())
                .senhaHash(passwordEncoder.encode(request.senhaDono())) // BCrypt — RNF01
                .perfil(Perfil.DONO)
                .build();
        usuarioRepository.save(dono);

        return farmacia;
    }
}
