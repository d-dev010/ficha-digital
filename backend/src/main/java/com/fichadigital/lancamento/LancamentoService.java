package com.fichadigital.lancamento;

import com.fichadigital.cliente.Cliente;
import com.fichadigital.cliente.ClienteRepository;
import com.fichadigital.usuario.Usuario;
import com.fichadigital.usuario.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Service de lançamentos de fiado.
 *
 * Regra crítica (RNF10 + RNF03):
 *  - O saldo_devedor é atualizado atomicamente dentro de @Transactional
 *  - O cliente é carregado com SELECT ... FOR UPDATE (lock pessimista) para evitar race condition
 *    em lançamentos simultâneos do mesmo cliente
 *  - farmaciaId sempre vem do JWT/SecurityContext — NUNCA do body (RNF03)
 *  - A validação de que o cliente pertence à farmácia do usuário autenticado é OBRIGATÓRIA (RNF03)
 */
@Service
@RequiredArgsConstructor
public class LancamentoService {

    private final LancamentoRepository lancamentoRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;

    /**
     * Lança um fiado para o cliente (US05).
     *
     * Confirmo entendimento do RNF03: farmaciaId é extraído do Authentication no controller
     * e passado para este service. A query findByIdForUpdate verifica que o cliente pertence
     * à mesma farmácia ANTES de qualquer escrita.
     *
     * @param farmaciaId UUID da farmácia — extraído do JWT (RNF03)
     * @param usuarioId  UUID do usuário autenticado — do SecurityContext
     * @param clienteId  UUID do cliente
     * @param valor      Valor positivo do fiado
     * @param descricao  Descrição livre
     * @return Lancamento persistido
     */
    @Transactional
    public Lancamento lancar(UUID farmaciaId, UUID usuarioId, UUID clienteId,
                             BigDecimal valor, String descricao) {
        // Validação multi-tenant + lock pessimista (RNF03 + RNF10)
        Cliente cliente = clienteRepository.findByIdForUpdate(clienteId)
                .orElseThrow(() -> new EntityNotFoundException("Cliente não encontrado: " + clienteId));

        // RNF03: garante que o cliente pertence à farmácia do usuário autenticado
        if (!cliente.getFarmacia().getId().equals(farmaciaId)) {
            throw new SecurityException("Acesso negado: cliente não pertence à farmácia do usuário autenticado");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));

        Lancamento lancamento = Lancamento.builder()
                .cliente(cliente)
                .usuario(usuario)
                .valor(valor)
                .descricao(descricao)
                .build();

        // Atualização atômica do saldo dentro da mesma transação (RNF10)
        cliente.setSaldoDevedor(cliente.getSaldoDevedor().add(valor));
        clienteRepository.save(cliente);

        return lancamentoRepository.save(lancamento);
    }
}
