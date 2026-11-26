package com.ligiamanzano.salon.inventario.repository;

import com.ligiamanzano.salon.inventario.entity.ConteoFisicoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConteoFisicoRepository extends JpaRepository<ConteoFisicoEntity, Integer> {

    List<ConteoFisicoEntity> findAllByIdInventarioFisico(int idInventarioFisico);

    Optional<ConteoFisicoEntity> findFirstByIdInventarioFisicoAndIdProducto(int idInventarioFisico, int idProducto);
}
