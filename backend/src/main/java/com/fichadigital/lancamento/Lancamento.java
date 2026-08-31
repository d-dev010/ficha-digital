package com.fichadigital.lancamento;

import com.fichadigital.cliente.Cliente;
import com.fichadigital.usuario.Usuario;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Lançamento de fiado. Aumenta o saldo_devedor do cliente de forma transacional.
 * Registra quem lançou (usuario_id) para rastreabilidade (US05).
 *
 * Índice por cliente_id + data para performance do extrato (RNF08/RNF09).
 */
@Entity
@Table(name = "lancamento",
        indexes = {
                @Index(name = "idx_lancamento_cliente_data", columnList = "cliente_id, data DESC")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lancamento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @NotNull
    @DecimalMin(value = "0.01", message = "Valor do lançamento deve ser maior que zero")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(length = 500)
    private String descricao;

    /** Nome de quem fisicamente retirou o item (pode ser diferente do cliente). Opcional. */
    @Column(name = "pessoa_retirou", length = 255)
    private String pessoaRetirou;

    @NotNull
    @Column(nullable = false)
    @Builder.Default
    private Instant data = Instant.now();
}
