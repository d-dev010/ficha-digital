package com.fichadigital.config;

import com.fichadigital.usuario.Perfil;
import com.fichadigital.usuario.Usuario;
import com.fichadigital.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cria o Super Admin inicial automaticamente se não existir.
 * Substitui a inserção via Flyway, pois o pgBouncer no Neon bloqueia session vars na JDBC URL.
 */
@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;

    @Value("${ADMIN_EMAIL:admin@fichadigital.com}")
    private String adminEmail;

    @Value("${ADMIN_PASSWORD_HASH:$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy}")
    private String adminPasswordHash;

    @Override
    @Transactional
    public void run(String... args) {
        try {
            Usuario admin = usuarioRepository.findByEmail(adminEmail).orElse(null);

            if (admin == null) {
                // Cria se não existir
                admin = Usuario.builder()
                        .farmacia(null) // SUPER_ADMIN não tem farmácia
                        .nome("Super Admin")
                        .email(adminEmail)
                        .senhaHash(adminPasswordHash)
                        .perfil(Perfil.SUPER_ADMIN)
                        .ativo(true)
                        .build();
                usuarioRepository.save(admin);
                System.out.println("Super Admin criado com sucesso: " + adminEmail);
            } else {
                // Força atualização da senha se já existir (Self-healing)
                admin.setSenhaHash(adminPasswordHash);
                admin.setPerfil(Perfil.SUPER_ADMIN);
                admin.setAtivo(true);
                usuarioRepository.save(admin);
                System.out.println("Super Admin atualizado com sucesso: " + adminEmail);
            }
        } catch (Exception e) {
            System.out.println("Erro ao inicializar admin (pode ser concorrência): " + e.getMessage());
        }
    }
}
