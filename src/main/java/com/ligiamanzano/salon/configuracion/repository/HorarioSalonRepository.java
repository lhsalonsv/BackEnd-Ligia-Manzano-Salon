package com.ligiamanzano.salon.configuracion.repository;

import com.ligiamanzano.salon.configuracion.entity.HorarioSalonEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HorarioSalonRepository extends JpaRepository<HorarioSalonEntity, Short> {
}
