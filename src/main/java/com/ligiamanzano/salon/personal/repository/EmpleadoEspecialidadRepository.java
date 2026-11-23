package com.ligiamanzano.salon.personal.repository;

import com.ligiamanzano.salon.personal.entity.EmpleadoEspecialidadEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EmpleadoEspecialidadRepository extends JpaRepository<EmpleadoEspecialidadEntity, Integer> {

    List<EmpleadoEspecialidadEntity> findByIdEmpleadoOrderByIdEspecialidadAsc(Integer idEmpleado);

    @Modifying
    @Query("delete from EmpleadoEspecialidadEntity ee where ee.idEmpleado = :idEmpleado")
    void borrarDeEmpleado(@Param("idEmpleado") Integer idEmpleado);
}