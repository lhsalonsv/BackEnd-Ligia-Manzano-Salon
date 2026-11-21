package com.ligiamanzano.salon.ventas.repository;

import com.ligiamanzano.salon.ventas.entity.DetalleVentaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetalleVentaRepository extends JpaRepository<DetalleVentaEntity, Integer> {
}
