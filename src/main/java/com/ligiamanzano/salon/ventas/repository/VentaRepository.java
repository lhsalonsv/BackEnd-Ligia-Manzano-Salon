package com.ligiamanzano.salon.ventas.repository;

import com.ligiamanzano.salon.ventas.entity.VentaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VentaRepository extends JpaRepository<VentaEntity, Integer> {
}
