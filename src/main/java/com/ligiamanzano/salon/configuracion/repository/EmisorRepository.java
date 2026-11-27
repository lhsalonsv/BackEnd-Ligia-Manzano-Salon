package com.ligiamanzano.salon.configuracion.repository;

import com.ligiamanzano.salon.configuracion.entity.EmisorEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmisorRepository extends JpaRepository<EmisorEntity, Integer> {

    Optional<EmisorEntity> findFirstByOrderByIdEmisorAsc();
}
