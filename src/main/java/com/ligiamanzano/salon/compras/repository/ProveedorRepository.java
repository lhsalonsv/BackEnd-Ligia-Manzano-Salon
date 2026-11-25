package com.ligiamanzano.salon.compras.repository;

import com.ligiamanzano.salon.compras.entity.ProveedorEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProveedorRepository extends JpaRepository<ProveedorEntity, Integer> {

    Optional<ProveedorEntity> findFirstByNit(String nit);

    Optional<ProveedorEntity> findFirstByNrc(String nrc);
}