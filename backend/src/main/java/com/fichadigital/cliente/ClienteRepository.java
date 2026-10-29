package com.fichadigital.cliente;

import com.fichadigital.farmacia.ResumoFarmaciaResponse;
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
 * Optimistic Locking (@Version) para atualização segura do saldo_devedor (RNF10).
 */
@Repository
public interface ClienteRepository extends JpaRepository<Cliente, UUID> {

    /**
     * Verifica se o cliente pertence à farmácia — usado para validação de multi-tenant.
     */
    boolean existsByIdAndFarmaciaId(UUID id, UUID farmaciaId);

    /**
     * Listagem paginada padrão (quando nenhum termo de busca é informado).
     * Utiliza diretamente o índice composto (farmacia_id, anonimizado, nome)
     * sem o custo de unaccent, LIKE ou funções em runtime.
     */
    Page<Cliente> findByFarmaciaIdAndAnonimizadoFalseOrderByNomeAsc(UUID farmaciaId, Pageable pageable);

    /**
     * Busca paginada por nome (parcial, case-insensitive, sem acento), telefone ou CPF — US04.
     * Filtrada por farmaciaId — RNF03.
     */
    @Query(value = """
            SELECT c.* FROM cliente c
            WHERE c.farmacia_id = :farmaciaId
              AND c.anonimizado = false
              AND (
                    unaccent_immutable(lower(c.nome))    LIKE unaccent_immutable(lower(concat('%', :termo, '%')))
                 OR c.telefone LIKE concat('%', :termo, '%')
                 OR c.cpf     LIKE concat('%', :termo, '%')
              )
            ORDER BY
              CASE
                WHEN unaccent_immutable(lower(c.nome)) LIKE unaccent_immutable(lower(concat(:termo, '%'))) THEN 1
                WHEN unaccent_immutable(lower(c.nome)) LIKE unaccent_immutable(lower(concat('%', :termo, '%'))) THEN 2
                ELSE 3
              END ASC,
              c.nome ASC
            """,
            countQuery = """
            SELECT count(1) FROM cliente c
            WHERE c.farmacia_id = :farmaciaId
              AND c.anonimizado = false
              AND (
                    unaccent_immutable(lower(c.nome))    LIKE unaccent_immutable(lower(concat('%', :termo, '%')))
                 OR c.telefone LIKE concat('%', :termo, '%')
                 OR c.cpf     LIKE concat('%', :termo, '%')
              )
            """,
            nativeQuery = true)
    Page<Cliente> buscar(@Param("farmaciaId") UUID farmaciaId, @Param("termo") String termo, Pageable pageable);

    /**
     * Detalhe do cliente filtrado por farmácia (RNF03).
     * Usado também nas escritas de saldo — o Optimistic Lock (@Version) garante atomicidade sem bloquear o banco.
     */
    @Query("SELECT c FROM Cliente c WHERE c.id = :id AND c.farmacia.id = :farmaciaId")
    Optional<Cliente> findByIdAndFarmaciaId(@Param("id") UUID id, @Param("farmaciaId") UUID farmaciaId);

    /**
     * Agrega métricas do painel do DONO (clientes ativos e total a receber) em uma ÚNICA consulta no banco,
     * eliminando scans duplicados.
     */
    @Query("""
        SELECT new com.fichadigital.farmacia.ResumoFarmaciaResponse(
            COUNT(CASE WHEN c.saldoDevedor > 0 THEN 1 END),
            COALESCE(SUM(c.saldoDevedor), 0)
        )
        FROM Cliente c
        WHERE c.farmacia.id = :farmaciaId AND c.anonimizado = false
    """)
    ResumoFarmaciaResponse obterResumo(@Param("farmaciaId") UUID farmaciaId);

    /**
     * Conta clientes com saldo devedor > 0 (clientes ativos) para o painel do DONO.
     * Mantido por compatibilidade.
     */
    @Query("SELECT COUNT(c) FROM Cliente c WHERE c.farmacia.id = :farmaciaId AND c.saldoDevedor > 0")
    long contarClientesAtivos(@Param("farmaciaId") UUID farmaciaId);

    /**
     * Soma o saldo devedor total de todos os clientes da farmácia (total a receber).
     * Retorna 0 se não houver clientes. Mantido por compatibilidade.
     */
    @Query("SELECT COALESCE(SUM(c.saldoDevedor), 0) FROM Cliente c WHERE c.farmacia.id = :farmaciaId")
    java.math.BigDecimal somarSaldoDevedor(@Param("farmaciaId") UUID farmaciaId);
}
