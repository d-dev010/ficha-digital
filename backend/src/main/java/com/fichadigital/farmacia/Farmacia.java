package com.fichadigital.farmacia;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.UUID;

/**
 * Representa uma farmácia (tenant). Toda query é filtrada por farmacia_id (RNF03).
 */
@Entity
@Table(name = "farmacia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Farmacia {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Column(nullable = false)
    private String nome;

    @Column(unique = true, length = 14)
    private String cnpj;
}
