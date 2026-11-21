package com.ligiamanzano.salon.notificaciones.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tb_notificaciones_leidas")
@Getter
@Setter
public class NotificacionLeidaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idNotificacionLeida;
}
