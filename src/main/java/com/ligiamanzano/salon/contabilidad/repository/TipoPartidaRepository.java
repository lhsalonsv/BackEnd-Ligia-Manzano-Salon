package com.ligiamanzano.salon.contabilidad.repository;

import com.ligiamanzano.salon.contabilidad.entity.TipoPartidaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TipoPartidaRepository extends JpaRepository<TipoPartidaEntity, String> {

    List<TipoPartidaEntity> findAllByOrderByCodigoAsc();

    Optional<TipoPartidaEntity> findFirstByPredeterminadoTrue();
}
