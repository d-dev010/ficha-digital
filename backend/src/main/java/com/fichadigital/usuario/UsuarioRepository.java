package com.fichadigital.usuario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Usuario> findByFarmaciaId(UUID farmaciaId);

    long countByFarmaciaId(UUID farmaciaId);

    /** Retorna todos os usuários que não são SUPER_ADMIN (para listagem no admin). */
    @Query("SELECT u FROM Usuario u WHERE u.perfil <> com.fichadigital.usuario.Perfil.SUPER_ADMIN")
    List<Usuario> findAllNonAdmin();
}

