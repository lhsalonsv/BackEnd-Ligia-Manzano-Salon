package com.ligiamanzano.salon.contabilidad.repository;

import com.ligiamanzano.salon.contabilidad.entity.CuentaContableEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CuentaContableRepository extends JpaRepository<CuentaContableEntity, String> {

    List<CuentaContableEntity> findAllByOrderByCodigoAsc();

    boolean existsByCodigoPadre(String codigoPadre);
}
