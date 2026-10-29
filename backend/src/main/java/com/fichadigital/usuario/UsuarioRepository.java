package com.fichadigital.usuario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    /**
     * Retorna usuários de uma farmácia com JOIN FETCH para evitar N+1 ao acessar dados da farmácia.
     */
    @Query("SELECT u FROM Usuario u LEFT JOIN FETCH u.farmacia WHERE u.farmacia.id = :farmaciaId")
    List<Usuario> findByFarmaciaId(@Param("farmaciaId") UUID farmaciaId);

    long countByFarmaciaId(UUID farmaciaId);

    /**
     * Retorna todos os usuários que não são SUPER_ADMIN (para listagem no admin)
     * com a farmácia carregada em uma única query (elimina o problema N+1).
     */
    @Query("SELECT u FROM Usuario u LEFT JOIN FETCH u.farmacia WHERE u.perfil <> com.fichadigital.usuario.Perfil.SUPER_ADMIN")
    List<Usuario> findAllNonAdmin();
}
