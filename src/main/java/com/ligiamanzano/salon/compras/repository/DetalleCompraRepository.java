package com.ligiamanzano.salon.compras.repository;

import com.ligiamanzano.salon.compras.entity.DetalleCompraEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DetalleCompraRepository extends JpaRepository<DetalleCompraEntity, Integer> {

    List<DetalleCompraEntity> findAllByIdCompra(Integer idCompra);
}
