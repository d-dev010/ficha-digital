package com.fichadigital.cliente;

import com.fichadigital.farmacia.Farmacia;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Cliente da farmácia. O saldo devedor é mantido de forma transacional (RNF10).
 * CPF é armazenado completo mas mascarado nas listagens (RNF04).
 *
 * Índices em nome, telefone e cpf para suportar o volume do RNF09 (5.000+ clientes).
 */
@Entity
@Table(name = "cliente",
        indexes = {
                @Index(name = "idx_cliente_farmacia_nome", columnList = "farmacia_id, nome"),
                @Index(name = "idx_cliente_farmacia_telefone", columnList = "farmacia_id, telefone"),
                @Index(name = "idx_cliente_farmacia_cpf", columnList = "farmacia_id, cpf")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "farmacia_id", nullable = false)
    private Farmacia farmacia;

    @NotBlank
    @Column(nullable = false)
    private String nome;

    private String telefone;

    /** CPF opcional. Armazenado completo; mascarado na camada de service ao listar (RNF04). */
    private String cpf;

    /**
     * Versão para Optimistic Locking (Problema 3 — Performance).
     * Substitui o SELECT FOR UPDATE pessimista. O Spring lança ObjectOptimisticLockingFailureException
     * em caso de escrita concorrente, que é tratada com retry na camada de service.
     */
    @Version
    @Column(nullable = false)
    private Long version;

    /**
     * Saldo devedor atual. Atualizado de forma transacional em LancamentoService
     * e PagamentoService via @Transactional + optimistic lock (RNF10).
     */
    @NotNull
    @Column(name = "saldo_devedor", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal saldoDevedor = BigDecimal.ZERO;
}
