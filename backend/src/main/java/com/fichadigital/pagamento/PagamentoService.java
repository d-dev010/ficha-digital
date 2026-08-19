package com.fichadigital.pagamento;

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
 * Service de pagamentos.
 *
 * Regra crítica (RNF10 + RNF03):
 *  - Abatimento do saldo_devedor é atômico — dentro de @Transactional
 *  - Lock pessimista (SELECT ... FOR UPDATE) evita race condition com lançamentos simultâneos
 *  - farmaciaId sempre vem do JWT/SecurityContext — NUNCA do body (RNF03)
 *  - Saldo não pode ficar negativo (proteção contra input inválido)
 */
@Service
@RequiredArgsConstructor
public class PagamentoService {

    private final PagamentoRepository pagamentoRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;

    /**
     * Registra um pagamento (total ou parcial) para o cliente (US07).
     *
     * Confirmo entendimento do RNF03: farmaciaId é extraído do Authentication no controller.
     *
     * @param farmaciaId UUID da farmácia — extraído do JWT (RNF03)
     * @param usuarioId  UUID do usuário autenticado — do SecurityContext
     * @param clienteId  UUID do cliente
     * @param valor      Valor positivo do pagamento
     * @return Pagamento persistido
     */
    @Transactional
    public Pagamento registrar(UUID farmaciaId, UUID usuarioId, UUID clienteId, BigDecimal valor) {
        // Lock pessimista — evita race condition com lançamentos simultâneos (RNF10)
        Cliente cliente = clienteRepository.findByIdForUpdate(clienteId)
                .orElseThrow(() -> new EntityNotFoundException("Cliente não encontrado: " + clienteId));

        // RNF03: garante que o cliente pertence à farmácia do usuário autenticado
        if (!cliente.getFarmacia().getId().equals(farmaciaId)) {
            throw new SecurityException("Acesso negado: cliente não pertence à farmácia do usuário autenticado");
        }

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
        cliente.setSaldoDevedor(novoSaldo);
        clienteRepository.save(cliente);

        return pagamentoRepository.save(pagamento);
    }
}
