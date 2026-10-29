package com.fichadigital.relatorio;

import com.fichadigital.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
 * Controller de relatórios gerais da farmácia.
 *
 * GET /relatorio/csv — exporta CSV com todas as movimentações de todos os
 *                      clientes da farmácia autenticada.
 *
 * Parâmetros opcionais:
 *   mes  (1–12)  — filtra pelo mês
 *   ano  (ex: 2026) — filtra pelo ano
 *
 * Sem parâmetros → exporta todo o histórico.
 *
 * Restrito ao perfil DONO (RNF03).
 * farmaciaId sempre do JWT via SecurityUtils — nunca de query param (RNF03).
 */
@RestController
@RequestMapping("/relatorio")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DONO')")
public class RelatorioController {

    private final EntityManager em;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.of("America/Sao_Paulo"));
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.of("America/Sao_Paulo"));

    /**
     * GET /relatorio/csv?mes=10&ano=2026
     * Exporta CSV geral da farmácia — todos os clientes.
     * farmaciaId do JWT (RNF03). Restrito ao DONO.
     *
     * @param mes  Mês (1–12) — opcional
     * @param ano  Ano (ex: 2026) — opcional
     * @return Arquivo CSV para download
     */
    @GetMapping(value = "/csv", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<byte[]> exportarCsv(
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
                SELECT cliente,
                       tipo,
                       valor,
                       descricao,
                       data,
                       nome_usuario,
                       pessoa_retirou
                  FROM (
                         SELECT c.nome          AS cliente,
                                'LANCAMENTO'    AS tipo,
                                l.valor,
                                l.descricao,
                                l.data,
                                u.nome          AS nome_usuario,
                                l.pessoa_retirou
                           FROM lancamento l
                           JOIN cliente c  ON c.id = l.cliente_id
                           JOIN usuario u  ON u.id = l.usuario_id
                          WHERE c.farmacia_id = :farmaciaId
                          """ + filtroMesLanc + """
                         UNION ALL
                         SELECT c.nome          AS cliente,
                                'PAGAMENTO'     AS tipo,
                                p.valor,
                                NULL            AS descricao,
                                p.data,
                                u.nome          AS nome_usuario,
                                NULL            AS pessoa_retirou
                           FROM pagamento p
                           JOIN cliente c  ON c.id = p.cliente_id
                           JOIN usuario u  ON u.id = p.usuario_id
                          WHERE c.farmacia_id = :farmaciaId
                          """ + filtroMesPag + """
                        ) AS mov
                  ORDER BY data DESC
                 """;

        Query query = em.createNativeQuery(sql)
                .setParameter("farmaciaId", farmaciaId);

        if (filtrarPeriodo) {
            query.setParameter("dataInicio", dataInicio).setParameter("dataFim", dataFim);
        }

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        StringBuilder csv = new StringBuilder();
        // BOM UTF-8 + diretiva sep para Excel abrir com ; como separador automaticamente (PT-BR)
        csv.append('\uFEFF');
        csv.append("sep=;\n");
        csv.append("Cliente;Tipo;Data;Hora;Valor (R$);Descricao;Responsavel;Pessoa que Retirou\n");

        for (Object[] r : rows) {
            Instant data = toInstant(r[4]);
            String tipo    = "LANCAMENTO".equals(r[1]) ? "Fiado" : "Pagamento";
            String valor   = formatValor((BigDecimal) r[2]);
            String descricao   = csvField((String) r[3]);
            String responsavel = csvField((String) r[5]);
            String retirou     = csvField((String) r[6]);

            csv.append(csvField((String) r[0])).append(';')
               .append(tipo).append(';')
               .append(DATE_FMT.format(data)).append(';')
               .append(TIME_FMT.format(data)).append(';')
               .append(valor).append(';')
               .append(descricao).append(';')
               .append(responsavel).append(';')
               .append(retirou).append('\n');
        }

        byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);

        String periodo = filtrarPeriodo
                ? String.format("%02d-%d", mes, ano)
                : "total";
        String filename = "relatorio-" + periodo + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .contentLength(bytes.length)
                .body(bytes);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Instant toInstant(Object value) {
        if (value instanceof Instant i)         return i;
        if (value instanceof java.time.OffsetDateTime odt) return odt.toInstant();
        return ((java.sql.Timestamp) value).toInstant();
    }

    private String formatValor(BigDecimal v) {
        return v == null ? "0,00" : v.toPlainString().replace('.', ',');
    }

    /** Envolve o campo em aspas e escapa aspas internas (RFC 4180). */
    private String csvField(String value) {
        if (value == null || value.isBlank()) return "";
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}
