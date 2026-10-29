package com.fichadigital.extrato;

import com.fichadigital.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
                SELECT id, tipo, valor, descricao, data, nome_usuario, pessoa_retirou
                  FROM (
                         SELECT l.id::text        AS id,
                                'LANCAMENTO'      AS tipo,
                                l.valor,
                                l.descricao,
                                l.data,
                                u.nome            AS nome_usuario,
                                l.pessoa_retirou
                           FROM lancamento l
                           JOIN cliente c ON c.id = l.cliente_id
                           JOIN usuario u ON u.id = l.usuario_id
                          WHERE c.id = :clienteId
                            AND c.farmacia_id = :farmaciaId
                         UNION ALL
                         SELECT p.id::text        AS id,
                                'PAGAMENTO'       AS tipo,
                                p.valor,
                                NULL              AS descricao,
                                p.data,
                                u.nome            AS nome_usuario,
                                NULL              AS pessoa_retirou
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

        List<ExtratoItem> itens = rows.stream().map(r -> {
            Instant data;
            if (r[4] instanceof Instant i) {
                data = i;
            } else if (r[4] instanceof java.time.OffsetDateTime odt) {
                data = odt.toInstant();
            } else {
                data = ((java.sql.Timestamp) r[4]).toInstant();
            }

            return new ExtratoItem(
                    UUID.fromString((String) r[0]),
                    TipoMovimento.valueOf((String) r[1]),
                    (BigDecimal) r[2],
                    (String) r[3],
                    data,
                    (String) r[5],
                    (String) r[6]
            );
        }).toList();

        return ResponseEntity.ok(new PageImpl<>(itens, pageable, total));
    }

    /**
     * GET /clientes/{clienteId}/extrato/csv?mes=10&ano=2026
     * Exporta o extrato completo (ou mensal) do cliente em formato CSV.
     *
     * Parâmetros opcionais:
     *   mes  (1–12)  — filtra pelo mês
     *   ano  (ex: 2026) — filtra pelo ano
     *
     * Sem parâmetros → exporta todo o histórico do cliente.
     * farmaciaId do JWT (RNF03).
     *
     * @param clienteId UUID do cliente
     * @param mes       Mês (1–12) — opcional
     * @param ano       Ano (ex: 2026) — opcional
     * @return Arquivo CSV para download
     */
    @GetMapping("/csv")
    public ResponseEntity<byte[]> exportarCsv(
            @PathVariable UUID clienteId,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer ano) {

        UUID farmaciaId = SecurityUtils.farmaciaId(); // RNF03

        boolean filtrarPeriodo = mes != null && ano != null;

        Instant dataInicio = null;
        Instant dataFim = null;
        if (filtrarPeriodo) {
            YearMonth ym = YearMonth.of(ano, mes);
            ZoneId zone = ZoneId.of("America/Sao_Paulo");
            dataInicio = ym.atDay(1).atStartOfDay(zone).toInstant();
            dataFim = ym.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant();
        }

        // Filtro sargável por data (data >= inicio AND data < fim) para permitir uso de índices
        String filtroMesLanc = filtrarPeriodo ? "AND l.data >= :dataInicio AND l.data < :dataFim" : "";
        String filtroMesPag  = filtrarPeriodo ? "AND p.data >= :dataInicio AND p.data < :dataFim" : "";

        String sql = """
                SELECT tipo, valor, descricao, data, nome_usuario, pessoa_retirou
                  FROM (
                         SELECT 'LANCAMENTO'      AS tipo,
                                l.valor,
                                l.descricao,
                                l.data,
                                u.nome            AS nome_usuario,
                                l.pessoa_retirou
                           FROM lancamento l
                           JOIN cliente c ON c.id = l.cliente_id
                           JOIN usuario u ON u.id = l.usuario_id
                          WHERE c.id = :clienteId
                            AND c.farmacia_id = :farmaciaId
                          """ + filtroMesLanc + """
                         UNION ALL
                         SELECT 'PAGAMENTO'       AS tipo,
                                p.valor,
                                NULL              AS descricao,
                                p.data,
                                u.nome            AS nome_usuario,
                                NULL              AS pessoa_retirou
                           FROM pagamento p
                           JOIN cliente c ON c.id = p.cliente_id
                           JOIN usuario u ON u.id = p.usuario_id
                          WHERE c.id = :clienteId
                            AND c.farmacia_id = :farmaciaId
                          """ + filtroMesPag + """
                        ) AS movimentacoes
                  ORDER BY data DESC
                 """;

        Query query = em.createNativeQuery(sql)
                .setParameter("clienteId", clienteId)
                .setParameter("farmaciaId", farmaciaId);

        if (filtrarPeriodo) {
            query.setParameter("dataInicio", dataInicio).setParameter("dataFim", dataFim);
        }

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                .withZone(ZoneId.of("America/Sao_Paulo"));
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm")
                .withZone(ZoneId.of("America/Sao_Paulo"));

        StringBuilder csv = new StringBuilder();
        // BOM UTF-8 + diretiva sep para Excel abrir com ; como separador automaticamente (PT-BR)
        csv.append('\uFEFF');
        csv.append("sep=;\n");
        csv.append("Tipo;Data;Hora;Valor (R$);Descricao;Responsavel;Pessoa que Retirou\n");

        for (Object[] r : rows) {
            Instant data;
            if (r[3] instanceof Instant i) {
                data = i;
            } else if (r[3] instanceof java.time.OffsetDateTime odt) {
                data = odt.toInstant();
            } else {
                data = ((java.sql.Timestamp) r[3]).toInstant();
            }

            String tipo        = "LANCAMENTO".equals(r[0]) ? "Fiado" : "Pagamento";
            String valor       = r[1] == null ? "0,00" : ((BigDecimal) r[1]).toPlainString().replace('.', ',');
            String descricao   = csvField((String) r[2]);
            String responsavel = csvField((String) r[4]);
            String retirou     = csvField((String) r[5]);

            csv.append(tipo).append(';')
               .append(dateFmt.format(data)).append(';')
               .append(timeFmt.format(data)).append(';')
               .append(valor).append(';')
               .append(descricao).append(';')
               .append(responsavel).append(';')
               .append(retirou).append('\n');
        }

        byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);

        String periodo = filtrarPeriodo
                ? String.format("%02d-%d", mes, ano)
                : "completo";
        String filename = "extrato-" + periodo + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .contentLength(bytes.length)
                .body(bytes);
    }

    /** Envolve o campo em aspas e escapa aspas internas (RFC 4180). */
    private String csvField(String value) {
        if (value == null || value.isBlank()) return "";
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
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
            String nomeResponsavel,
            String pessoaRetirou
    ) {}
}
