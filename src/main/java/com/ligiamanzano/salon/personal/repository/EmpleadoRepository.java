package com.ligiamanzano.salon.personal.repository;

import com.ligiamanzano.salon.personal.entity.EmpleadoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmpleadoRepository extends JpaRepository<EmpleadoEntity, Integer> {

    Optional<EmpleadoEntity> findFirstByDui(String dui);

    Optional<EmpleadoEntity> findFirstByIdUsuario(Integer idUsuario);
}