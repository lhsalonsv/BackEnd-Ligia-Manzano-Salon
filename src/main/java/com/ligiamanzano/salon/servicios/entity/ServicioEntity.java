package com.ligiamanzano.salon.servicios.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "tb_servicios")
@Getter
@Setter
public class ServicioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_servicio")
    private Integer idServicio;

    @Column(name = "nombre_servicio", nullable = false, length = 80)
    private String nombreServicio;

    @Column(name = "descripcion", length = 300)
    private String descripcion;

    @Column(name = "id_especialidad", nullable = false)
    private Integer idEspecialidad;

    @Column(name = "duracion_min")
    private Short duracionMin;

    @Column(name = "precio", nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    @Column(name = "precio_desde", nullable = false)
    private boolean precioDesde;

    @Column(name = "activo", nullable = false)
    private boolean activo;
}
