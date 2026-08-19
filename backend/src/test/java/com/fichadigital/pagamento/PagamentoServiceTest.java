package com.fichadigital.pagamento;

import com.fichadigital.cliente.Cliente;
import com.fichadigital.cliente.ClienteRepository;
import com.fichadigital.farmacia.Farmacia;
import com.fichadigital.usuario.Perfil;
import com.fichadigital.usuario.Usuario;
import com.fichadigital.usuario.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para PagamentoService.
 * Cobre: abatimento correto, pagamento maior que saldo (floor em zero), cross-tenant (RNF03).
 */
@ExtendWith(MockitoExtension.class)
class PagamentoServiceTest {

    @Mock
    private PagamentoRepository pagamentoRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private PagamentoService pagamentoService;

    private UUID farmaciaId;
    private UUID usuarioId;
    private UUID clienteId;
    private Farmacia farmacia;
    private Cliente cliente;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        farmaciaId = UUID.randomUUID();
        usuarioId = UUID.randomUUID();
        clienteId = UUID.randomUUID();

        farmacia = Farmacia.builder().id(farmaciaId).nome("Farmácia Teste").build();

        cliente = Cliente.builder()
                .id(clienteId)
                .farmacia(farmacia)
                .nome("Maria Oliveira")
                .saldoDevedor(new BigDecimal("200.00"))
                .build();

        usuario = Usuario.builder()
                .id(usuarioId)
                .farmacia(farmacia)
                .nome("Atendente 1")
                .email("atendente@farmacia.com")
                .senhaHash("hash")
                .perfil(Perfil.ATENDENTE)
                .build();
    }

    @Test
    @DisplayName("Deve registrar pagamento e abater saldo corretamente")
    void deveAbaterSaldoCorretamente() {
        // Arrange
        BigDecimal valorPagamento = new BigDecimal("80.00");
        when(clienteRepository.findByIdForUpdate(clienteId)).thenReturn(Optional.of(cliente));
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(pagamentoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // Act
        Pagamento pagamento = pagamentoService.registrar(farmaciaId, usuarioId, clienteId, valorPagamento);

        // Assert
        assertThat(pagamento.getValor()).isEqualByComparingTo("80.00");
        assertThat(cliente.getSaldoDevedor()).isEqualByComparingTo("120.00");
        verify(clienteRepository).save(cliente);
    }

    @Test
    @DisplayName("Deve zerar saldo quando pagamento é maior que o saldo devedor")
    void deveZerarSaldoQuandoPagamentoMaiorQueSaldo() {
        // Arrange: cliente deve 200.00, pagamento de 300.00
        BigDecimal pagamentoExcessivo = new BigDecimal("300.00");
        when(clienteRepository.findByIdForUpdate(clienteId)).thenReturn(Optional.of(cliente));
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(pagamentoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // Act
        pagamentoService.registrar(farmaciaId, usuarioId, clienteId, pagamentoExcessivo);

        // Assert: saldo não pode ficar negativo — piso em zero
        assertThat(cliente.getSaldoDevedor()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Deve lançar EntityNotFoundException quando cliente não encontrado")
    void deveLancarExcecaoQuandoClienteNaoEncontrado() {
        when(clienteRepository.findByIdForUpdate(clienteId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                pagamentoService.registrar(farmaciaId, usuarioId, clienteId, BigDecimal.TEN))
                .isInstanceOf(EntityNotFoundException.class);

        verify(pagamentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve bloquear pagamento para cliente de outra farmácia (RNF03)")
    void deveBloquearAcessoCrossTenant() {
        UUID outraFarmaciaId = UUID.randomUUID();
        Farmacia outraFarmacia = Farmacia.builder().id(outraFarmaciaId).nome("Outra").build();
        Cliente clienteAlheio = Cliente.builder()
                .id(clienteId).farmacia(outraFarmacia)
                .nome("X").saldoDevedor(BigDecimal.ZERO).build();

        when(clienteRepository.findByIdForUpdate(clienteId)).thenReturn(Optional.of(clienteAlheio));

        assertThatThrownBy(() ->
                pagamentoService.registrar(farmaciaId, usuarioId, clienteId, BigDecimal.TEN))
                .isInstanceOf(SecurityException.class);

        verify(pagamentoRepository, never()).save(any());
    }
}
