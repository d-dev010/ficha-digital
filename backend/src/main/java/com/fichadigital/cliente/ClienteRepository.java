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
     * Busca paginada por nome (parcial, case-insensitive, sem acento), telefone ou CPF — US04.
     * Filtrada por farmaciaId — RNF03.
     *
     * Melhorias em relação à versão anterior:
     *  1. unaccent(): "e" encontra "é", "a" encontra "ã", "c" encontra "ç", etc.
     *  2. ORDER BY de relevância:
     *       - prioridade 1: nome começa com o termo (starts-with)
     *       - prioridade 2: nome contém o termo em outra posição
     *       - prioridade 3: match apenas por telefone/CPF
     *     Dentro de cada nível, ordena alfabeticamente por nome.
     */
    @Query(value = """
            SELECT c.* FROM clientes c
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
            SELECT count(c.*) FROM clientes c
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
     * Conta clientes com saldo devedor > 0 (clientes ativos) para o painel do DONO.
     * Filtrado por farmácia — RNF03.
     */
    @Query("SELECT COUNT(c) FROM Cliente c WHERE c.farmacia.id = :farmaciaId AND c.saldoDevedor > 0")
    long contarClientesAtivos(@Param("farmaciaId") UUID farmaciaId);

    /**
     * Soma o saldo devedor total de todos os clientes da farmácia (total a receber).
     * Retorna 0 se não houver clientes. Filtrado por farmácia — RNF03.
     */
    @Query("SELECT COALESCE(SUM(c.saldoDevedor), 0) FROM Cliente c WHERE c.farmacia.id = :farmaciaId")
    java.math.BigDecimal somarSaldoDevedor(@Param("farmaciaId") UUID farmaciaId);
}
