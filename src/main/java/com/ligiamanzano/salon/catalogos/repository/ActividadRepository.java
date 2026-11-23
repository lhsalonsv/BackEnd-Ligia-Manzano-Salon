package com.ligiamanzano.salon.catalogos.repository;

import com.ligiamanzano.salon.catalogos.entity.ActividadEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActividadRepository extends JpaRepository<ActividadEntity, String> {

    List<ActividadEntity> findAllByOrderByDescripcionAsc();
}
