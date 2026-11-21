package com.ligiamanzano.salon.inventario.repository;

import com.ligiamanzano.salon.inventario.entity.LoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoteRepository extends JpaRepository<LoteEntity, Integer> {
}
