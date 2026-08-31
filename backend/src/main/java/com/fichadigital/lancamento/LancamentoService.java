package com.fichadigital.lancamento;

import com.fichadigital.cliente.Cliente;
import com.fichadigital.cliente.ClienteRepository;
import com.fichadigital.usuario.Usuario;
import com.fichadigital.usuario.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Service de lançamentos de fiado.
 *
 * Regra crítica (RNF10 + RNF03):
 *  - O saldo_devedor é atualizado atomicamente dentro de @Transactional
 *  - Optimistic Locking via @Version na entidade Cliente (Problema 3 — Performance):
 *    substitui o lock pessimista (SELECT FOR UPDATE), eliminando o bloqueio de linha no banco.
 *    Em caso de escrita concorrente, ObjectOptimisticLockingFailureException é capturada
 *    e a operação é repetida até MAX_RETRIES vezes.
 *  - farmaciaId sempre vem do JWT/SecurityContext — NUNCA do body (RNF03)
 *  - A validação de que o cliente pertence à farmácia do usuário autenticado é OBRIGATÓRIA (RNF03)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LancamentoService {

    private static final int MAX_RETRIES = 3;

    private final LancamentoRepository lancamentoRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;

    @Autowired
    @Lazy
    private LancamentoService self;

    /**
     * Lança um fiado para o cliente (US05).
     *
     * Confirmo entendimento do RNF03: farmaciaId é extraído do Authentication no controller
     * e passado para este service. A query findByIdAndFarmaciaId verifica que o cliente pertence
     * à mesma farmácia ANTES de qualquer escrita.
     *
     * Usa retry para lidar com ObjectOptimisticLockingFailureException (Problema 3).
     *
     * @param farmaciaId UUID da farmácia — extraído do JWT (RNF03)
     * @param usuarioId  UUID do usuário autenticado — do SecurityContext
     * @param clienteId  UUID do cliente
     * @param valor      Valor positivo do fiado
     * @param descricao  Descrição livre
     * @return Lancamento persistido
     */
    public Lancamento lancar(UUID farmaciaId, UUID usuarioId, UUID clienteId,
                             BigDecimal valor, String descricao, String pessoaRetirou) {
        int tentativas = 0;
        while (true) {
            try {
                // Problema C2: Chama via 'self' para passar pelo proxy do Spring e ativar o @Transactional
                return self.tentarLancar(farmaciaId, usuarioId, clienteId, valor, descricao, pessoaRetirou);
            } catch (ObjectOptimisticLockingFailureException e) {
                tentativas++;
                if (tentativas >= MAX_RETRIES) {
                    log.error("Falha ao lançar fiado após {} tentativas por conflito de concorrência (clienteId={})",
                            MAX_RETRIES, clienteId);
                    throw e;
                }
                log.warn("Conflito de escrita concorrente ao lançar fiado (tentativa {}/{}), reexecutando...",
                        tentativas, MAX_RETRIES);
            }
        }
    }

    @Transactional
    public Lancamento tentarLancar(UUID farmaciaId, UUID usuarioId, UUID clienteId,
                                    BigDecimal valor, String descricao, String pessoaRetirou) {
        // Validação multi-tenant com Optimistic Lock via @Version (RNF03 + RNF10)
        Cliente cliente = clienteRepository.findByIdAndFarmaciaId(clienteId, farmaciaId)
                .orElseThrow(() -> new EntityNotFoundException("Cliente não encontrado: " + clienteId));

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));

        Lancamento lancamento = Lancamento.builder()
                .cliente(cliente)
                .usuario(usuario)
                .valor(valor)
                .descricao(descricao)
                .pessoaRetirou(pessoaRetirou)
                .build();

        // Atualização atômica do saldo dentro da mesma transação (RNF10)
        // O @Version na entidade Cliente detecta colisões e lança ObjectOptimisticLockingFailureException
        cliente.setSaldoDevedor(cliente.getSaldoDevedor().add(valor));
        clienteRepository.save(cliente);

        return lancamentoRepository.save(lancamento);
    }
}
