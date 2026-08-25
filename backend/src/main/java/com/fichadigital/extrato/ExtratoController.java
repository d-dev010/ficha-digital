package com.fichadigital.extrato;

import com.fichadigital.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Controller de extrato.
 * GET /clientes/{id}/extrato — US08
 *
 * Problema 1 (OOM/Performance) — CORRIGIDO:
 *  - A junção de lançamentos e pagamentos agora é feita no banco via UNION ALL (query nativa).
 *  - Paginação aplicada diretamente no banco com LIMIT/OFFSET.
 *  - Ordenação por data DESC feita pelo banco, não mais em memória Java.
 *  - Resultado: custo de memória O(page_size) em vez de O(total_movimentações_do_cliente).
 *
 * farmaciaId sempre do JWT via SecurityUtils (RNF03).
 */
@RestController
@RequestMapping("/clientes/{clienteId}/extrato")
@RequiredArgsConstructor
public class ExtratoController {

    private final EntityManager em;

    /**
     * GET /clientes/{clienteId}/extrato?page=0&size=50
     * Retorna o extrato paginado do cliente (lançamentos + pagamentos) ordenados por data DESC (US08).
     * farmaciaId do JWT — nunca do body (RNF03).
     *
     * @param clienteId UUID do cliente
     * @param page      Número da página (0-indexed, padrão: 0)
     * @param size      Tamanho da página (padrão: 50, máximo: 200)
     * @return Página de ExtratoItem ordenada por data DESC
     */
    @GetMapping
    public ResponseEntity<Page<ExtratoItem>> extrato(
            @PathVariable UUID clienteId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        UUID farmaciaId = SecurityUtils.farmaciaId(); // RNF03
        Pageable pageable = PageRequest.of(page, Math.min(size, 200));

        // Query nativa UNION ALL — junção e ordenação feitas no banco (Problema 1)
        String sql = """
                SELECT id, 'LANCAMENTO' AS tipo, valor, descricao, data, nome_usuario
                  FROM (
                         SELECT l.id::text   AS id,
                                l.valor,
                                l.descricao,
                                l.data,
                                u.nome       AS nome_usuario
                           FROM lancamento l
                           JOIN cliente c ON c.id = l.cliente_id
                           JOIN usuario u ON u.id = l.usuario_id
                          WHERE c.id = :clienteId
                            AND c.farmacia_id = :farmaciaId
                         UNION ALL
                         SELECT p.id::text   AS id,
                                p.valor,
                                NULL          AS descricao,
                                p.data,
                                u.nome        AS nome_usuario
                           FROM pagamento p
                           JOIN cliente c ON c.id = p.cliente_id
                           JOIN usuario u ON u.id = p.usuario_id
                          WHERE c.id = :clienteId
                            AND c.farmacia_id = :farmaciaId
                       ) AS movimentacoes
                 ORDER BY data DESC
                 LIMIT :limit OFFSET :offset
                """;

        String countSql = """
                SELECT COUNT(*) FROM (
                  SELECT 1 FROM lancamento l
                    JOIN cliente c ON c.id = l.cliente_id
                   WHERE c.id = :clienteId AND c.farmacia_id = :farmaciaId
                  UNION ALL
                  SELECT 1 FROM pagamento p
                    JOIN cliente c ON c.id = p.cliente_id
                   WHERE c.id = :clienteId AND c.farmacia_id = :farmaciaId
                ) AS total
                """;

        Query dataQuery = em.createNativeQuery(sql)
                .setParameter("clienteId", clienteId)
                .setParameter("farmaciaId", farmaciaId)
                .setParameter("limit", pageable.getPageSize())
                .setParameter("offset", pageable.getOffset());

        Query countQuery = em.createNativeQuery(countSql)
                .setParameter("clienteId", clienteId)
                .setParameter("farmaciaId", farmaciaId);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = dataQuery.getResultList();
        long total = ((Number) countQuery.getSingleResult()).longValue();

        List<ExtratoItem> itens = rows.stream().map(r -> new ExtratoItem(
                UUID.fromString((String) r[0]),
                TipoMovimento.valueOf((String) r[1]),
                (BigDecimal) r[2],
                (String) r[3],
                ((java.sql.Timestamp) r[4]).toInstant(),
                (String) r[5]
        )).toList();

        return ResponseEntity.ok(new PageImpl<>(itens, pageable, total));
    }

    public enum TipoMovimento { LANCAMENTO, PAGAMENTO }

    /**
     * DTO de item do extrato.
     * tipo indica se é lançamento (débito) ou pagamento (crédito).
     */
    public record ExtratoItem(
            UUID id,
            TipoMovimento tipo,
            BigDecimal valor,
            String descricao,
            Instant data,
            String nomeResponsavel
    ) {}
}
