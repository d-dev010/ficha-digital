package com.fichadigital.cliente;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
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
     * Busca por nome (parcial, case-insensitive), telefone ou CPF — US04.
     * Filtrada por farmaciaId — RNF03.
     */
    @Query("""
            SELECT c FROM Cliente c
            WHERE c.farmacia.id = :farmaciaId
              AND (
                    LOWER(c.nome) LIKE LOWER(CONCAT('%', :termo, '%'))
                 OR c.telefone LIKE CONCAT('%', :termo, '%')
                 OR c.cpf LIKE CONCAT('%', :termo, '%')
              )
            ORDER BY c.nome ASC
            """)
    List<Cliente> buscar(@Param("farmaciaId") UUID farmaciaId, @Param("termo") String termo);

    /**
     * Carrega o cliente com SELECT ... FOR UPDATE para atualização transacional do saldo (RNF10).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Cliente c WHERE c.id = :id")
    Optional<Cliente> findByIdForUpdate(@Param("id") UUID id);

    /**
     * Detalhe do cliente filtrado por farmácia (RNF03).
     */
    @Query("SELECT c FROM Cliente c WHERE c.id = :id AND c.farmacia.id = :farmaciaId")
    Optional<Cliente> findByIdAndFarmaciaId(@Param("id") UUID id, @Param("farmaciaId") UUID farmaciaId);
}
