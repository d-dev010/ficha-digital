package com.fichadigital.cliente;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository de clientes.
 *
 * Multi-tenant: todos os métodos recebem farmaciaId extraído do JWT — nunca do request body (RNF03).
 * Busca combinada por nome/telefone/CPF para US04.
 * Lock pessimista em findByIdForUpdate para atualização segura do saldo_devedor (RNF10).
 */
@Repository
public interface ClienteRepository extends JpaRepository<Cliente, UUID> {

    /**
     * Verifica se o cliente pertence à farmácia — usado para validação de multi-tenant.
     */
    boolean existsByIdAndFarmaciaId(UUID id, UUID farmaciaId);

    /**
     * Busca paginada por nome (parcial, case-insensitive), telefone ou CPF — US04 (Problema 2 — Performance).
     * Filtrada por farmaciaId — RNF03. Ordenação e limitação feitas pelo banco via Pageable.
     */
    @Query("""
            SELECT c FROM Cliente c
            WHERE c.farmacia.id = :farmaciaId
              AND (
                    LOWER(c.nome) LIKE LOWER(CONCAT('%', :termo, '%'))
                 OR c.telefone LIKE CONCAT('%', :termo, '%')
                 OR c.cpf LIKE CONCAT('%', :termo, '%')
              )
            """)
    Page<Cliente> buscar(@Param("farmaciaId") UUID farmaciaId, @Param("termo") String termo, Pageable pageable);

    /**
     * Detalhe do cliente filtrado por farmácia (RNF03).
     * Usado também nas escritas de saldo — o Optimistic Lock (@Version) garante atomicidade sem bloquear o banco.
     */
    @Query("SELECT c FROM Cliente c WHERE c.id = :id AND c.farmacia.id = :farmaciaId")
    Optional<Cliente> findByIdAndFarmaciaId(@Param("id") UUID id, @Param("farmaciaId") UUID farmaciaId);
}
