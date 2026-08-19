package com.fichadigital.pagamento;

import com.fichadigital.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller de pagamentos.
 * POST /clientes/{id}/pagamentos — US07
 *
 * farmaciaId e usuarioId sempre do JWT via SecurityUtils (RNF03).
 */
@RestController
@RequestMapping("/clientes/{clienteId}/pagamentos")
@RequiredArgsConstructor
public class PagamentoController {

    private final PagamentoService pagamentoService;

    /**
     * POST /clientes/{clienteId}/pagamentos
     * Registra um pagamento (total ou parcial) do cliente (US07).
     * Abate saldo_devedor de forma transacional e com lock pessimista (RNF10).
     *
     * @param clienteId Path variable — UUID do cliente
     * @param request   Valor do pagamento
     * @return 201 Created com PagamentoResponse
     */
    @PostMapping
    public ResponseEntity<PagamentoResponse> registrar(
            @PathVariable UUID clienteId,
            @Valid @RequestBody RegistrarPagamentoRequest request) {

        UUID farmaciaId = SecurityUtils.farmaciaId(); // RNF03 — nunca do body
        UUID usuarioId = SecurityUtils.usuarioId();

        Pagamento pagamento = pagamentoService.registrar(farmaciaId, usuarioId, clienteId, request.valor());

        return ResponseEntity.status(HttpStatus.CREATED).body(PagamentoResponse.from(pagamento));
    }
}
