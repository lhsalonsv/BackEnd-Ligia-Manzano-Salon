package com.ligiamanzano.salon.notificaciones.repository;

import com.ligiamanzano.salon.notificaciones.entity.NotificacionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificacionRepository extends JpaRepository<NotificacionEntity, Integer> {
}
