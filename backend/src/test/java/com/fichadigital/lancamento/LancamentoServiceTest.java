package com.fichadigital.lancamento;

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
 * Testes unitários para LancamentoService.
 * Cobre: lançamento normal, cliente não encontrado, violação de farmacia_id (RNF03).
 */
@ExtendWith(MockitoExtension.class)
class LancamentoServiceTest {

    @Mock
    private LancamentoRepository lancamentoRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private LancamentoService lancamentoService;

    private UUID farmaciaId;
    private UUID outraFarmaciaId;
    private UUID usuarioId;
    private UUID clienteId;
    private Farmacia farmacia;
    private Cliente cliente;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        farmaciaId = UUID.randomUUID();
        outraFarmaciaId = UUID.randomUUID();
        usuarioId = UUID.randomUUID();
        clienteId = UUID.randomUUID();

        farmacia = Farmacia.builder().id(farmaciaId).nome("Farmácia Teste").build();

        cliente = Cliente.builder()
                .id(clienteId)
                .farmacia(farmacia)
                .nome("João Silva")
                .saldoDevedor(BigDecimal.ZERO)
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
    @DisplayName("Deve lançar fiado e atualizar saldo devedor corretamente")
    void deveLancarFiadoEAtualizarSaldo() {
        // Arrange
        BigDecimal valorFiado = new BigDecimal("50.00");
        when(clienteRepository.findByIdForUpdate(clienteId)).thenReturn(Optional.of(cliente));
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(lancamentoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // Act
        Lancamento resultado = lancamentoService.lancar(
                farmaciaId, usuarioId, clienteId, valorFiado, "Remédio X");

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.getValor()).isEqualByComparingTo("50.00");
        assertThat(resultado.getDescricao()).isEqualTo("Remédio X");
        assertThat(cliente.getSaldoDevedor()).isEqualByComparingTo("50.00");
        verify(clienteRepository).save(cliente);
        verify(lancamentoRepository).save(any(Lancamento.class));
    }

    @Test
    @DisplayName("Deve acumular saldo em múltiplos lançamentos")
    void deveAcumularSaldo() {
        // Arrange
        cliente.setSaldoDevedor(new BigDecimal("100.00"));
        BigDecimal novoLancamento = new BigDecimal("30.50");
        when(clienteRepository.findByIdForUpdate(clienteId)).thenReturn(Optional.of(cliente));
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(lancamentoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // Act
        lancamentoService.lancar(farmaciaId, usuarioId, clienteId, novoLancamento, "desc");

        // Assert
        assertThat(cliente.getSaldoDevedor()).isEqualByComparingTo("130.50");
    }

    @Test
    @DisplayName("Deve lançar EntityNotFoundException quando cliente não encontrado")
    void deveLancarExcecaoQuandoClienteNaoEncontrado() {
        // Arrange
        when(clienteRepository.findByIdForUpdate(clienteId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() ->
                lancamentoService.lancar(farmaciaId, usuarioId, clienteId, BigDecimal.TEN, "desc"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Cliente não encontrado");

        verify(lancamentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar SecurityException quando cliente pertence a outra farmácia (RNF03)")
    void deveLancarExcecaoQuandoClienteDeOutraFarmacia() {
        // Arrange: cliente pertence a outraFarmaciaId, mas usuário autenticado tem farmaciaId
        Farmacia outraFarmacia = Farmacia.builder().id(outraFarmaciaId).nome("Outra Farmácia").build();
        Cliente clienteDeOutraFarmacia = Cliente.builder()
                .id(clienteId)
                .farmacia(outraFarmacia)
                .nome("Maria")
                .saldoDevedor(BigDecimal.ZERO)
                .build();

        when(clienteRepository.findByIdForUpdate(clienteId))
                .thenReturn(Optional.of(clienteDeOutraFarmacia));

        // Act & Assert — RNF03: deve bloquear acesso cross-tenant
        assertThatThrownBy(() ->
                lancamentoService.lancar(farmaciaId, usuarioId, clienteId, BigDecimal.TEN, "desc"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Acesso negado");

        verify(clienteRepository, never()).save(any());
        verify(lancamentoRepository, never()).save(any());
    }
}
