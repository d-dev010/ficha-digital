package com.fichadigital.config;

import com.fichadigital.usuario.Perfil;
import com.fichadigital.usuario.Usuario;
import com.fichadigital.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cria/atualiza o Super Admin na inicialização da aplicação.
 * O hash é gerado pelo próprio Spring BCrypt — não depende de variável externa com hash pré-calculado.
 */
@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${ADMIN_EMAIL:admin@fichadigital.com}")
    private String adminEmail;

    // Senha em texto puro — o Spring vai transformar em BCrypt na hora
    @Value("${ADMIN_PASSWORD:admin123}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(String... args) {
        try {
            // Gera o hash BCrypt diretamente, sem depender de variável externa com hash pré-calculado
            String hash = passwordEncoder.encode(adminPassword);

            Usuario admin = usuarioRepository.findByEmail(adminEmail).orElse(null);

            if (admin == null) {
                // Cria novo Super Admin
                admin = Usuario.builder()
                        .farmacia(null)
                        .nome("Super Admin")
                        .email(adminEmail)
                        .senhaHash(hash)
                        .perfil(Perfil.SUPER_ADMIN)
                        .ativo(true)
                        .build();
                usuarioRepository.save(admin);
                System.out.println("[AdminInitializer] Super Admin CRIADO: " + adminEmail);
            } else {
                // Atualiza senha e garante perfil correto (self-healing)
                admin.setSenhaHash(hash);
                admin.setPerfil(Perfil.SUPER_ADMIN);
                admin.setAtivo(true);
                usuarioRepository.save(admin);
                System.out.println("[AdminInitializer] Super Admin ATUALIZADO: " + adminEmail);
            }
        } catch (Exception e) {
            System.err.println("[AdminInitializer] Erro: " + e.getMessage());
        }
    }
}
