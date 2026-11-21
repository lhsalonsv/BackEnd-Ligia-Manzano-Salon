package com.ligiamanzano.salon.usuarios.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tb_sesiones")
@Getter
@Setter
public class SesionEntity {

    @Id
    @Column(name = "id_sesion")
    private UUID idSesion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private UsuarioEntity usuario;

    @Column(name = "creada_en", nullable = false)
    private Instant creadaEn;

    @Column(name = "ultima_actividad", nullable = false)
    private Instant ultimaActividad;

    @Column(name = "expira_en", nullable = false)
    private Instant expiraEn;

    @Column(name = "revocada_en")
    private Instant revocadaEn;

    @Column(name = "motivo_revocacion", length = 30)
    private String motivoRevocacion;

    @Column(name = "ip", length = 45)
    private String ip;

    @Column(name = "user_agent", length = 300)
    private String userAgent;
}
