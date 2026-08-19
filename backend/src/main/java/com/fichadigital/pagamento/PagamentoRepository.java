package com.fichadigital.pagamento;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PagamentoRepository extends JpaRepository<Pagamento, UUID> {

    /**
     * Busca todos os pagamentos de um cliente ordenados por data DESC — para o extrato (US08).
     * Valida que o cliente pertence à farmácia do usuário autenticado (RNF03).
     */
    @Query("""
            SELECT p FROM Pagamento p
            JOIN p.cliente c
            WHERE c.id = :clienteId
              AND c.farmacia.id = :farmaciaId
            ORDER BY p.data DESC
            """)
    List<Pagamento> findByClienteIdAndFarmaciaId(
            @Param("clienteId") UUID clienteId,
            @Param("farmaciaId") UUID farmaciaId);
}
