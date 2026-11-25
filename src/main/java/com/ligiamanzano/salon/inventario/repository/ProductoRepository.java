package com.ligiamanzano.salon.inventario.repository;

import com.ligiamanzano.salon.inventario.entity.ProductoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductoRepository extends JpaRepository<ProductoEntity, Integer> {

    //Para evitar productos duplicados
    Optional<ProductoEntity> findFirstByCodigoIgnoreCase(String codigo);
}