package com.ligiamanzano.salon.caja.repository;

import com.ligiamanzano.salon.caja.entity.CajaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CajaRepository extends JpaRepository<CajaEntity, Integer> {
}
