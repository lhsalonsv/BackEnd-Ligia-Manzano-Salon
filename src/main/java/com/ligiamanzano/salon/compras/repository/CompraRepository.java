package com.ligiamanzano.salon.compras.repository;

import com.ligiamanzano.salon.compras.entity.CompraEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompraRepository extends JpaRepository<CompraEntity, Integer> {
}
