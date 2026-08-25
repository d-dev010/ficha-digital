package com.fichadigital.pagamento;

import com.fichadigital.cliente.Cliente;
import com.fichadigital.cliente.ClienteRepository;
import com.fichadigital.usuario.Usuario;
import com.fichadigital.usuario.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Service de pagamentos.
 *
 * Regra crítica (RNF10 + RNF03):
 *  - Abatimento do saldo_devedor é atômico — dentro de @Transactional
 *  - Optimistic Locking via @Version na entidade Cliente (Problema 3 — Performance):
 *    substitui o lock pessimista (SELECT FOR UPDATE), eliminando o bloqueio de linha no banco.
 *    Em caso de escrita concorrente, ObjectOptimisticLockingFailureException é capturada
 *    e a operação é repetida até MAX_RETRIES vezes.
 *  - farmaciaId sempre vem do JWT/SecurityContext — NUNCA do body (RNF03)
 *  - Saldo não pode ficar negativo (proteção contra input inválido)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PagamentoService {

    private static final int MAX_RETRIES = 3;

    private final PagamentoRepository pagamentoRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;

    /**
     * Registra um pagamento (total ou parcial) para o cliente (US07).
     *
     * Confirmo entendimento do RNF03: farmaciaId é extraído do Authentication no controller.
     * Usa retry para lidar com ObjectOptimisticLockingFailureException (Problema 3).
     *
     * @param farmaciaId UUID da farmácia — extraído do JWT (RNF03)
     * @param usuarioId  UUID do usuário autenticado — do SecurityContext
     * @param clienteId  UUID do cliente
     * @param valor      Valor positivo do pagamento
     * @return Pagamento persistido
     */
    public Pagamento registrar(UUID farmaciaId, UUID usuarioId, UUID clienteId, BigDecimal valor) {
        int tentativas = 0;
        while (true) {
            try {
                return tentarRegistrar(farmaciaId, usuarioId, clienteId, valor);
            } catch (ObjectOptimisticLockingFailureException e) {
                tentativas++;
                if (tentativas >= MAX_RETRIES) {
                    log.error("Falha ao registrar pagamento após {} tentativas por conflito de concorrência (clienteId={})",
                            MAX_RETRIES, clienteId);
                    throw e;
                }
                log.warn("Conflito de escrita concorrente ao registrar pagamento (tentativa {}/{}), reexecutando...",
                        tentativas, MAX_RETRIES);
            }
        }
    }

    @Transactional
    private Pagamento tentarRegistrar(UUID farmaciaId, UUID usuarioId, UUID clienteId, BigDecimal valor) {
        // Optimistic Lock via @Version — sem bloquear a linha do banco (RNF10 + Problema 3)
        Cliente cliente = clienteRepository.findByIdAndFarmaciaId(clienteId, farmaciaId)
                .orElseThrow(() -> new EntityNotFoundException("Cliente não encontrado: " + clienteId));

        // Proteção: pagamento maior que o saldo devedor abate até zero
        BigDecimal novoSaldo = cliente.getSaldoDevedor().subtract(valor);
        if (novoSaldo.compareTo(BigDecimal.ZERO) < 0) {
            novoSaldo = BigDecimal.ZERO;
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));

        Pagamento pagamento = Pagamento.builder()
                .cliente(cliente)
                .usuario(usuario)
                .valor(valor)
                .build();

        // Atualização atômica do saldo dentro da mesma transação (RNF10)
        // O @Version na entidade Cliente detecta colisões e lança ObjectOptimisticLockingFailureException
        cliente.setSaldoDevedor(novoSaldo);
        clienteRepository.save(cliente);

        return pagamentoRepository.save(pagamento);
    }
}
