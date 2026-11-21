package com.ligiamanzano.salon.planilla.repository;

import com.ligiamanzano.salon.planilla.entity.PlanillaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanillaRepository extends JpaRepository<PlanillaEntity, Integer> {
}
