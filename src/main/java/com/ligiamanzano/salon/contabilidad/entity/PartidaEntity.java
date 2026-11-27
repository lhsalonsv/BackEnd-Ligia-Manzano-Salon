package com.ligiamanzano.salon.contabilidad.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "tb_partidas")
@Getter
@Setter
public class PartidaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_partida")
    private Integer idPartida;

    @Column(name = "anio", nullable = false)
    private Short anio;

    @Column(name = "mes", nullable = false)
    private Short mes;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "numero", length = 6, insertable = false, updatable = false)
    private String numero;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "codigo_tipo", nullable = false, length = 3)
    private String codigoTipo;

    @Column(name = "concepto", nullable = false, length = 300)
    private String concepto;

    @Column(name = "estado", nullable = false, length = 10, insertable = false, updatable = false)
    private String estado;

    @Column(name = "origen", nullable = false, length = 20, insertable = false, updatable = false)
    private String origen;

    @Column(name = "id_partida_anula", insertable = false, updatable = false)
    private Integer idPartidaAnula;

    @Column(name = "id_usuario_crea", nullable = false, updatable = false)
    private Integer idUsuarioCrea;

    @Column(name = "creado_en", nullable = false, insertable = false, updatable = false)
    private Instant creadoEn;

    @Column(name = "id_usuario_aplica", insertable = false, updatable = false)
    private Integer idUsuarioAplica;

    @Column(name = "aplicada_en", insertable = false, updatable = false)
    private Instant aplicadaEn;
}
