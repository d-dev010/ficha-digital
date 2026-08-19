package com.fichadigital.lancamento;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LancamentoRepository extends JpaRepository<Lancamento, UUID> {

    /**
     * Busca todos os lançamentos de um cliente ordenados por data DESC — para o extrato (US08).
     * Valida que o cliente pertence à farmácia do usuário autenticado (RNF03).
     */
    @Query("""
            SELECT l FROM Lancamento l
            JOIN l.cliente c
            WHERE c.id = :clienteId
              AND c.farmacia.id = :farmaciaId
            ORDER BY l.data DESC
            """)
    List<Lancamento> findByClienteIdAndFarmaciaId(
            @Param("clienteId") UUID clienteId,
            @Param("farmaciaId") UUID farmaciaId);
}
