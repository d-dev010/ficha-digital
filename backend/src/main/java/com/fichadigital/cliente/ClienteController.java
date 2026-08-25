package com.fichadigital.cliente;

import com.fichadigital.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller de clientes.
 *
 * POST   /clientes           — cadastrar cliente (US03)
 * GET    /clientes?busca=    — busca por nome/telefone/CPF (US04)
 * GET    /clientes/{id}      — detalhe do cliente (US03)
 *
 * farmaciaId sempre do JWT via SecurityUtils (RNF03).
 */
@RestController
@RequestMapping("/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService clienteService;

    /**
     * POST /clientes
     * Cadastra um novo cliente. CPF é opcional.
     * farmaciaId do JWT — nunca do body (RNF03).
     *
     * @return 201 Created com ClienteDetalhe (CPF completo permitido aqui — é o próprio cadastro).
     */
    @PostMapping
    public ResponseEntity<ClienteDetalhe> cadastrar(@Valid @RequestBody CadastrarClienteRequest request) {
        UUID farmaciaId = SecurityUtils.farmaciaId(); // RNF03
        Cliente cliente = clienteService.cadastrar(farmaciaId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ClienteDetalhe.from(cliente));
    }

    /**
     * GET /clientes?busca={termo}&page=0&size=20
     * Busca paginada de clientes por nome (parcial), telefone ou CPF — US04 (Problema 2 — Performance).
     * CPF mascarado no resultado — RNF04.
     * farmaciaId do JWT — nunca do body (RNF03).
     *
     * @param busca Termo de busca (nome parcial, telefone ou CPF)
     * @param page  Número da página (0-indexed, padrão: 0)
     * @param size  Tamanho da página (padrão: 20, máximo recomendado: 100)
     * @return Página de ClienteResumo com CPF mascarado.
     */
    @GetMapping
    public ResponseEntity<Page<ClienteResumo>> buscar(
            @RequestParam(required = false, defaultValue = "") String busca,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID farmaciaId = SecurityUtils.farmaciaId(); // RNF03
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("nome").ascending());
        return ResponseEntity.ok(clienteService.buscar(farmaciaId, busca, pageable));
    }

    /**
     * GET /clientes/{id}
     * Detalhe completo do cliente com saldo atual.
     * CPF completo (apenas no detalhe — RNF04).
     * farmaciaId do JWT — nunca do body (RNF03).
     *
     * @return ClienteDetalhe ou 404 se não encontrado na farmácia.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ClienteDetalhe> detalhe(@PathVariable UUID id) {
        UUID farmaciaId = SecurityUtils.farmaciaId(); // RNF03
        return ResponseEntity.ok(clienteService.detalhe(farmaciaId, id));
    }
}
