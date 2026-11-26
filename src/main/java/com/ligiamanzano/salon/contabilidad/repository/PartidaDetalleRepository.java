package com.ligiamanzano.salon.contabilidad.repository;

import com.ligiamanzano.salon.contabilidad.entity.PartidaDetalleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartidaDetalleRepository extends JpaRepository<PartidaDetalleEntity, Long> {

    boolean existsByCodigoCuenta(String codigoCuenta);
}
