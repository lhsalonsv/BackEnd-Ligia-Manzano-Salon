package com.ligiamanzano.salon.catalogos.repository;

import com.ligiamanzano.salon.catalogos.entity.ActividadEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActividadRepository extends JpaRepository<ActividadEntity, String> {
}
