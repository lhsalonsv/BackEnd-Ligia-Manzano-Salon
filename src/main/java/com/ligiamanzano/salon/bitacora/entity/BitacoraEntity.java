package com.ligiamanzano.salon.bitacora.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "tb_bitacora")
@Getter
@Setter
public class BitacoraEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_bitacora")
    private Long idBitacora;

    @Column(name = "fecha_hora", nullable = false, insertable = false, updatable = false)
    private Instant fechaHora;

    @Column(name = "id_usuario")
    private Integer idUsuario;

    @Column(name = "modulo", nullable = false, length = 30)
    private String modulo;

    @Column(name = "accion", nullable = false, length = 30)
    private String accion;

    @Column(name = "entidad", length = 40)
    private String entidad;

    @Column(name = "entidad_id", length = 40)
    private String entidadId;

    @Column(name = "descripcion", nullable = false, length = 300)
    private String descripcion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "datos_antes")
    private String datosAntes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "datos_despues")
    private String datosDespues;

    @Column(name = "equipo", length = 120)
    private String equipo;
}
