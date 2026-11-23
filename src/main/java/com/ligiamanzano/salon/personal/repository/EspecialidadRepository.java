package com.ligiamanzano.salon.personal.repository;

import com.ligiamanzano.salon.personal.entity.EspecialidadEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public interface EspecialidadRepository extends JpaRepository<EspecialidadEntity, Integer> {

    long countByIdEspecialidadIn(Collection<Integer> ids);
}