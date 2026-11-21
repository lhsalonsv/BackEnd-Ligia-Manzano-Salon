package com.ligiamanzano.salon.inventario.repository;

import com.ligiamanzano.salon.inventario.entity.CategoriaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<CategoriaEntity, Integer> {
}
