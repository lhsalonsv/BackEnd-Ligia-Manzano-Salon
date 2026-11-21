package com.ligiamanzano.salon.personal.repository;

import com.ligiamanzano.salon.personal.entity.EmpleadoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmpleadoRepository extends JpaRepository<EmpleadoEntity, Integer> {
}
