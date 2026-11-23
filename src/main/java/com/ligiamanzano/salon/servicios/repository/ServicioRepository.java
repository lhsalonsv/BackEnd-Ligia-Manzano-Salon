package com.ligiamanzano.salon.servicios.repository;

import com.ligiamanzano.salon.servicios.entity.ServicioEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServicioRepository extends JpaRepository<ServicioEntity, Integer> {

    List<ServicioEntity> findAllByOrderByNombreServicioAsc();

    Optional<ServicioEntity> findFirstByNombreServicioIgnoreCase(String nombreServicio);
}
