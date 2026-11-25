package com.ligiamanzano.salon.notificaciones.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "tb_notificaciones")
@Getter
@Setter
public class NotificacionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_notificacion")
    private Integer idNotificacion;

    @Column(name = "tipo", nullable = false, length = 30)
    private String tipo;

    @Column(name = "titulo", nullable = false, length = 120)
    private String titulo;

    @Column(name = "mensaje", length = 300)
    private String mensaje;

    @Column(name = "entidad", length = 40)
    private String entidad;

    @Column(name = "entidad_id", length = 40)
    private String entidadId;

    @Column(name = "id_rol_destino")
    private Short idRolDestino;

    @Column(name = "creada_en", nullable = false, insertable = false, updatable = false)
    private Instant creadaEn;
}