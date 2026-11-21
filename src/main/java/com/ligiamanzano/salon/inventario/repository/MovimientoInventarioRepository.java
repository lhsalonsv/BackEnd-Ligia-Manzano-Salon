package com.ligiamanzano.salon.inventario.repository;

import com.ligiamanzano.salon.inventario.entity.MovimientoInventarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventarioEntity, Long> {
}
