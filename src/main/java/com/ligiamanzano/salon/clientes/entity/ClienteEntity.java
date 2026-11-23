package com.ligiamanzano.salon.clientes.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "tb_clientes")
@Getter
@Setter
public class ClienteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cliente")
    private Integer idCliente;

    @Column(name = "nombre", nullable = false, length = 80)
    private String nombre;

    @Column(name = "apellido", length = 80)
    private String apellido;

    @Column(name = "telefono", nullable = false, length = 30)
    private String telefono;

    @Column(name = "correo", length = 100)
    private String correo;

    @Column(name = "dia_cumple")
    private Short diaCumple;

    @Column(name = "mes_cumple")
    private Short mesCumple;

    @Column(name = "alergias", length = 300)
    private String alergias;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "tipo_documento", length = 2)
    private String tipoDocumento;

    @Column(name = "num_documento", length = 20)
    private String numDocumento;

    @Column(name = "razon_social", length = 250)
    private String razonSocial;

    @Column(name = "nit", length = 14)
    private String nit;

    @Column(name = "nrc", length = 8)
    private String nrc;

    @Column(name = "nombre_comercial", length = 150)
    private String nombreComercial;

    @Column(name = "cod_actividad", length = 6)
    private String codActividad;

    @Column(name = "id_distrito")
    private Integer idDistrito;

    @Column(name = "complemento", length = 200)
    private String complemento;

    @Column(name = "activo", nullable = false)
    private boolean activo;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;
}