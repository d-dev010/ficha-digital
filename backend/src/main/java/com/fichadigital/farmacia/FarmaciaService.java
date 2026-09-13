package com.fichadigital.farmacia;

import com.fichadigital.cliente.ClienteRepository;
import com.fichadigital.usuario.Perfil;
import com.fichadigital.usuario.Usuario;
import com.fichadigital.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

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
    private final ClienteRepository clienteRepository;

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

    /**
     * Agrega métricas do painel administrativo do DONO (US — painel adm).
     * Retorna: clientes com saldo devedor > 0 e soma total dos saldos devedores.
     *
     * @param farmaciaId UUID da farmácia — extraído do JWT (RNF03)
     * @return ResumoFarmaciaResponse com clientesAtivos e totalAReceber
     */
    @Transactional(readOnly = true)
    public ResumoFarmaciaResponse resumo(UUID farmaciaId) {
        long clientesAtivos = clienteRepository.contarClientesAtivos(farmaciaId);
        java.math.BigDecimal totalAReceber = clienteRepository.somarSaldoDevedor(farmaciaId);
        return new ResumoFarmaciaResponse(clientesAtivos, totalAReceber);
    }
}
