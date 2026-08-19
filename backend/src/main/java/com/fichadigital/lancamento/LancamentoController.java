package com.fichadigital.lancamento;

import com.fichadigital.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller de lançamentos de fiado.
 * POST /clientes/{id}/lancamentos — US05
 *
 * farmaciaId e usuarioId sempre do JWT via SecurityUtils (RNF03).
 */
@RestController
@RequestMapping("/clientes/{clienteId}/lancamentos")
@RequiredArgsConstructor
public class LancamentoController {

    private final LancamentoService lancamentoService;

    /**
     * POST /clientes/{clienteId}/lancamentos
     * Lança um fiado para o cliente (US05).
     * Atualiza saldo_devedor de forma transacional e com lock pessimista (RNF10).
     *
     * @param clienteId Path variable — UUID do cliente
     * @param request   Valor + descrição livre
     * @return 201 Created com LancamentoResponse
     */
    @PostMapping
    public ResponseEntity<LancamentoResponse> lancar(
            @PathVariable UUID clienteId,
            @Valid @RequestBody LancarFiadoRequest request) {

        UUID farmaciaId = SecurityUtils.farmaciaId(); // RNF03 — nunca do body
        UUID usuarioId = SecurityUtils.usuarioId();

        Lancamento lancamento = lancamentoService.lancar(
                farmaciaId, usuarioId, clienteId, request.valor(), request.descricao());

        return ResponseEntity.status(HttpStatus.CREATED).body(LancamentoResponse.from(lancamento));
    }
}
