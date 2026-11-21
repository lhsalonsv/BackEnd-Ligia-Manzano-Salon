package com.ligiamanzano.salon.planilla.repository;

import com.ligiamanzano.salon.planilla.entity.DetallePlanillaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetallePlanillaRepository extends JpaRepository<DetallePlanillaEntity, Integer> {
}
