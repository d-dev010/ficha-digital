package com.fichadigital.pagamento;

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
 * Pagamento (total ou parcial) realizado pelo cliente.
 * Abate o saldo_devedor de forma transacional (RNF10).
 * Registra quem registrou o pagamento (usuario_id) para rastreabilidade (US07).
 *
 * Índice por cliente_id + data para o extrato (RNF08/RNF09).
 */
@Entity
@Table(name = "pagamento",
        indexes = {
                @Index(name = "idx_pagamento_cliente_data", columnList = "cliente_id, data DESC")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pagamento {

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
    @DecimalMin(value = "0.01", message = "Valor do pagamento deve ser maior que zero")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @NotNull
    @Column(nullable = false)
    @Builder.Default
    private Instant data = Instant.now();
}
