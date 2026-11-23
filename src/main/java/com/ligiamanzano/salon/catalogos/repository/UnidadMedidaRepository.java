package com.ligiamanzano.salon.catalogos.repository;

import com.ligiamanzano.salon.catalogos.entity.UnidadMedidaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UnidadMedidaRepository extends JpaRepository<UnidadMedidaEntity, Short> {

    List<UnidadMedidaEntity> findAllByOrderByCodUnidadMedidaAsc();

}
