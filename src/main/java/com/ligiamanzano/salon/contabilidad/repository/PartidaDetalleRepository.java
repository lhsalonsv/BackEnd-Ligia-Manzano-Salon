package com.ligiamanzano.salon.contabilidad.repository;

import com.ligiamanzano.salon.contabilidad.entity.PartidaDetalleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface PartidaDetalleRepository extends JpaRepository<PartidaDetalleEntity, Long> {

    @Modifying
    @Query("delete from PartidaDetalleEntity d where d.idPartida = :idPartida")
    void borrarDeLaPartida(int idPartida);

    boolean existsByCodigoCuenta(String codigoCuenta);
}
