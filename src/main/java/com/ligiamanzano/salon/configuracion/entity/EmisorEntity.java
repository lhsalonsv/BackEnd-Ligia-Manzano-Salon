package com.ligiamanzano.salon.configuracion.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;

@Entity
@Table(name = "tb_emisores")
@Getter
@Setter
public class EmisorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_emisor")
    private Integer idEmisor;

    @Column(name = "nit", nullable = false, length = 14)
    private String nit;

    @Column(name = "nrc", nullable = false, length = 8)
    private String nrc;

    @Column(name = "nombre", nullable = false, length = 250)
    private String nombre;

    @Column(name = "nombre_comercial", length = 150)
    private String nombreComercial;

    @Column(name = "cod_actividad", nullable = false, length = 6)
    private String codActividad;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "tipo_establecimiento", nullable = false, length = 2)
    private String tipoEstablecimiento;

    @Column(name = "id_distrito", nullable = false)
    private Integer idDistrito;

    @Column(name = "complemento", nullable = false, length = 200)
    private String complemento;

    @Column(name = "telefono", nullable = false, length = 30)
    private String telefono;

    @Column(name = "correo", nullable = false, length = 100)
    private String correo;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "cod_estable_mh", length = 4, insertable = false, updatable = false)
    private String codEstableMh;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "cod_estable", nullable = false, length = 4)
    private String codEstable;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "cod_punto_venta_mh", length = 4, insertable = false, updatable = false)
    private String codPuntoVentaMh;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "cod_punto_venta", nullable = false, length = 4)
    private String codPuntoVenta;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "ambiente", nullable = false, length = 2, insertable = false, updatable = false)
    private String ambiente;

    @Column(name = "certificado_vence", insertable = false, updatable = false)
    private LocalDate certificadoVence;

    @Column(name = "activo", nullable = false)
    private boolean activo;
}
