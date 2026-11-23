package com.ligiamanzano.salon.configuracion.repository;

import com.ligiamanzano.salon.configuracion.entity.HorarioSalonEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HorarioSalonRepository extends JpaRepository<HorarioSalonEntity, Short> {

    List<HorarioSalonEntity> findAllByOrderByDiaSemanaAsc();
}