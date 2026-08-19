package com.fichadigital.farmacia;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FarmaciaRepository extends JpaRepository<Farmacia, UUID> {

    Optional<Farmacia> findByCnpj(String cnpj);

    boolean existsByCnpj(String cnpj);
}
