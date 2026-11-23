package com.ligiamanzano.salon.personal.repository;

import com.ligiamanzano.salon.personal.entity.HorarioEmpleadoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HorarioEmpleadoRepository extends JpaRepository<HorarioEmpleadoEntity, Integer> {

    List<HorarioEmpleadoEntity> findByIdEmpleadoOrderByDiaSemanaAscHoraInicioAsc(Integer idEmpleado);

    /**
     * Al reemplazar el horario se borra y se inserta en la misma transacción.
     * Hibernate guarda las inserciones antes que los borrados derivados, y el unique fallaría.
     * Se hace con @Query en su lugar para que se ejecute en el momento.
     */
    @Modifying
    @Query("delete from HorarioEmpleadoEntity h where h.idEmpleado = :idEmpleado")
    void borrarDeEmpleado(@Param("idEmpleado") Integer idEmpleado);
}