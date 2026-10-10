package com.fichadigital.health;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Endpoint de health check — sem autenticação, sem acesso ao banco.
 *
 * Usado pelo GitHub Actions (keep-alive.yml) para manter a API acordada
 * no Render (plano gratuito hiberna após ~15 min sem requisições).
 *
 * GET /health → 200 { "status": "ok", "timestamp": "<ISO-8601>" }
 * GET /       → 200 { "status": "ok", "timestamp": "<ISO-8601>" } (mantido por compatibilidade)
 */
@RestController
public class HealthController {

    @GetMapping({"/", "/health"})
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status",    "ok",
                "timestamp", Instant.now().toString()
        ));
    }
}
