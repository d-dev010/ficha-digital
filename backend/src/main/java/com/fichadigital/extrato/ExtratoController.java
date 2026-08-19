package com.fichadigital.extrato;

import com.fichadigital.lancamento.Lancamento;
import com.fichadigital.lancamento.LancamentoRepository;
import com.fichadigital.pagamento.Pagamento;
import com.fichadigital.pagamento.PagamentoRepository;
import com.fichadigital.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Controller de extrato.
 * GET /clientes/{id}/extrato — US08
 *
 * Retorna lançamentos e pagamentos intercalados, ordenados por data DESC.
 * farmaciaId sempre do JWT via SecurityUtils (RNF03).
 */
@RestController
@RequestMapping("/clientes/{clienteId}/extrato")
@RequiredArgsConstructor
public class ExtratoController {

    private final LancamentoRepository lancamentoRepository;
    private final PagamentoRepository pagamentoRepository;

    /**
     * GET /clientes/{clienteId}/extrato
     * Retorna o extrato completo do cliente (lançamentos + pagamentos) ordenados por data DESC (US08).
     * farmaciaId do JWT — nunca do body (RNF03).
     *
     * @param clienteId UUID do cliente
     * @return Lista de ExtratoItem ordenada por data DESC
     */
    @GetMapping
    public ResponseEntity<List<ExtratoItem>> extrato(@PathVariable UUID clienteId) {
        UUID farmaciaId = SecurityUtils.farmaciaId(); // RNF03

        List<Lancamento> lancamentos = lancamentoRepository
                .findByClienteIdAndFarmaciaId(clienteId, farmaciaId);
        List<Pagamento> pagamentos = pagamentoRepository
                .findByClienteIdAndFarmaciaId(clienteId, farmaciaId);

        List<ExtratoItem> itens = new ArrayList<>();

        lancamentos.forEach(l -> itens.add(new ExtratoItem(
                l.getId(), TipoMovimento.LANCAMENTO,
                l.getValor(), l.getDescricao(),
                l.getData(), l.getUsuario().getNome()
        )));

        pagamentos.forEach(p -> itens.add(new ExtratoItem(
                p.getId(), TipoMovimento.PAGAMENTO,
                p.getValor(), null,
                p.getData(), p.getUsuario().getNome()
        )));

        itens.sort(Comparator.comparing(ExtratoItem::data).reversed());

        return ResponseEntity.ok(itens);
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
