package com.ligiamanzano.salon.inventario.repository;

import com.ligiamanzano.salon.inventario.entity.MarcaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MarcaRepository extends JpaRepository<MarcaEntity, Integer> {

    List<MarcaEntity> findAllByOrderByNombreMarcaAsc();

    //No se repite sin importar mayúsculas ni tildes
    @Query(value = """
            select * from tb_marcas
             where unaccent(lower(nombre_marca)) = unaccent(lower(:nombre))
             limit 1
            """, nativeQuery = true)
    Optional<MarcaEntity> buscarMismoNombre(@Param("nombre") String nombre);
}