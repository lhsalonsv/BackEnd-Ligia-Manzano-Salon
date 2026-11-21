package com.ligiamanzano.salon.usuarios.repository;

import com.ligiamanzano.salon.usuarios.entity.RolEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolRepository extends JpaRepository<RolEntity, Short> {
}
