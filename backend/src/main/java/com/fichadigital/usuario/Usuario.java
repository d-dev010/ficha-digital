package com.fichadigital.usuario;

import com.fichadigital.farmacia.Farmacia;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Usuário do sistema (dono ou atendente).
 * Implementa UserDetails para integração com Spring Security.
 * A senha nunca é armazenada em texto puro — sempre via bcrypt (RNF01).
 */
@Entity
@Table(name = "usuario",
        uniqueConstraints = @UniqueConstraint(name = "uk_usuario_email", columnNames = "email"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "farmacia_id", nullable = false)
    private Farmacia farmacia;

    @NotBlank
    @Column(nullable = false)
    private String nome;

    @Email
    @NotBlank
    @Column(nullable = false, unique = true)
    private String email;

    /**
     * Hash bcrypt da senha. Nunca texto puro, nunca logada (RNF01).
     */
    @NotBlank
    @Column(name = "senha_hash", nullable = false)
    private String senhaHash;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Perfil perfil;

    /**
     * Indica se o usuário está ativo (Problema 5 — Segurança).
     * Quando false, o JwtAuthFilter recusa a autenticação mesmo com token JWT válido,
     * permitindo revogar sessões instantaneamente ao demitir/excluir um funcionário.
     */
    @Builder.Default
    @Column(nullable = false)
    private boolean ativo = true;

    // ——— UserDetails ———

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + perfil.name()));
    }

    @Override
    public String getPassword() {
        return senhaHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        // Problema 5: retorna false para usuários desativados, bloqueando acesso mesmo com JWT válido
        return ativo;
    }
}
