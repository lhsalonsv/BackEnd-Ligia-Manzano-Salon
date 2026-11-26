package com.ligiamanzano.salon.contabilidad.repository;

import com.ligiamanzano.salon.contabilidad.entity.ConfigContableEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfigContableRepository extends JpaRepository<ConfigContableEntity, Short> {
}
