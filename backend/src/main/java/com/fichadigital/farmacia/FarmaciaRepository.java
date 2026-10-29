package com.fichadigital.farmacia;

import com.fichadigital.admin.AdminDtos;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FarmaciaRepository extends JpaRepository<Farmacia, UUID> {

    Optional<Farmacia> findByCnpj(String cnpj);

    boolean existsByCnpj(String cnpj);

    /**
     * Consulta agregada para o painel Super Admin:
     * Retorna todas as farmácias com o total de usuários em uma única consulta (elimina o problema N+1).
     */
    @Query("""
        SELECT new com.fichadigital.admin.AdminDtos$FarmaciaAdminResponse(
            f.id, f.nome, f.cnpj, COUNT(u)
        )
        FROM Farmacia f
        LEFT JOIN Usuario u ON u.farmacia = f
        GROUP BY f.id, f.nome, f.cnpj
        ORDER BY f.nome ASC
    """)
    List<AdminDtos.FarmaciaAdminResponse> findAllWithUserCount();
}
