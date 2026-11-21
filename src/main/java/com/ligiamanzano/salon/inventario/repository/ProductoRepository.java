package com.ligiamanzano.salon.inventario.repository;

import com.ligiamanzano.salon.inventario.entity.ProductoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoRepository extends JpaRepository<ProductoEntity, Integer> {
}
