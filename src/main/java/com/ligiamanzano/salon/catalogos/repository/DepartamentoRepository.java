package com.ligiamanzano.salon.catalogos.repository;

import com.ligiamanzano.salon.catalogos.entity.DepartamentoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartamentoRepository extends JpaRepository<DepartamentoEntity, String> {
}
