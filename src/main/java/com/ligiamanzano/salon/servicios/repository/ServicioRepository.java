package com.ligiamanzano.salon.servicios.repository;

import com.ligiamanzano.salon.servicios.entity.ServicioEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServicioRepository extends JpaRepository<ServicioEntity, Integer> {
}
