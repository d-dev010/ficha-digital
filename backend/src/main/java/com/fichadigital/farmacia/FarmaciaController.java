package com.fichadigital.farmacia;

import com.fichadigital.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller de farmácias — rota pública.
 * POST /farmacias — US01 (cadastro inicial da farmácia + dono)
 */
@RestController
@RequestMapping("/farmacias")
@RequiredArgsConstructor
public class FarmaciaController {

    private final FarmaciaService farmaciaService;

    /**
     * POST /farmacias
     * Cadastro inicial: cria a farmácia e o usuário DONO em uma única transação.
     *
     * @return 201 Created com o ID da farmácia criada.
     */
    @PostMapping
    public ResponseEntity<FarmaciaResponse> criar(@Valid @RequestBody CriarFarmaciaRequest request) {
        Farmacia farmacia = farmaciaService.criarFarmaciaComDono(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new FarmaciaResponse(farmacia.getId(), farmacia.getNome(), farmacia.getCnpj()));
    }

    /**
     * GET /farmacias/resumo
     * Retorna métricas do painel administrativo: clientes ativos e total a receber.
     * Restrito ao perfil DONO. farmaciaId do JWT — nunca do body (RNF03).
     *
     * @return ResumoFarmaciaResponse com clientesAtivos e totalAReceber
     */
    @GetMapping("/resumo")
    @PreAuthorize("hasRole('DONO')")
    public ResponseEntity<ResumoFarmaciaResponse> resumo() {
        UUID farmaciaId = SecurityUtils.farmaciaId(); // RNF03
        return ResponseEntity.ok(farmaciaService.resumo(farmaciaId));
    }

    /**
     * DTO de response — evita expor a entidade JPA diretamente.
     */
    public record FarmaciaResponse(UUID id, String nome, String cnpj) {}
}
