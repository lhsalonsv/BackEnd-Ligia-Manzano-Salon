package com.ligiamanzano.salon.catalogos.repository;

import com.ligiamanzano.salon.catalogos.dto.DistritoDTO;
import com.ligiamanzano.salon.catalogos.entity.DistritoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DistritoRepository extends JpaRepository<DistritoEntity, Integer> {

    @Query("""
        select new com.ligiamanzano.salon.catalogos.dto.DistritoDTO(
        d.idDistrito, d.nombre, m.nombre, dep.nombre)
        from DistritoEntity d
        join d.municipio m
        join m.departamento dep
        order by dep.nombre, m.nombre, d.nombre
    """)
    List<DistritoDTO> listarConMunicipioYDepartamento();
}
