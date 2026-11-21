package com.ligiamanzano.salon.bitacora.repository;

import com.ligiamanzano.salon.bitacora.entity.BitacoraEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BitacoraRepository extends JpaRepository<BitacoraEntity, Long> {
}
