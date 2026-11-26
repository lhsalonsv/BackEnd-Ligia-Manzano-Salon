package com.ligiamanzano.salon.inventario.repository;

import com.ligiamanzano.salon.inventario.entity.InventarioFisicoEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Optional;

public interface InventarioFisicoRepository extends JpaRepository<InventarioFisicoEntity, Integer> {

    boolean existsByFechaCorte(LocalDate fechaCorte);

    Optional<InventarioFisicoEntity> findFirstByEstado(String estado);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from InventarioFisicoEntity f where f.idInventarioFisico = :id")
    Optional<InventarioFisicoEntity> bloquear(int id);
}
