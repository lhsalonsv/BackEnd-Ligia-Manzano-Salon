package com.ligiamanzano.salon.compras.repository;

import com.ligiamanzano.salon.compras.entity.LiquidacionTarjetaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LiquidacionTarjetaRepository extends JpaRepository<LiquidacionTarjetaEntity, Integer> {

    Optional<LiquidacionTarjetaEntity> findFirstByCodigoGeneracion(UUID codigoGeneracion);
}
