package com.ligiamanzano.salon.inventario.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "tb_inventarios_fisicos")
@Getter
@Setter
public class InventarioFisicoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_inventario_fisico")
    private Integer idInventarioFisico;

    @Column(name = "fecha_corte", nullable = false, unique = true)
    private LocalDate fechaCorte;

    @Column(name = "estado", nullable = false, length = 10)
    private String estado;

    @Column(name = "observaciones", length = 500)
    private String observaciones;

    @Column(name = "id_usuario", nullable = false)
    private Integer idUsuario;

    @Column(name = "creado_en", nullable = false, insertable = false, updatable = false)
    private Instant creadoEn;

    @Column(name = "id_usuario_cierra")
    private Integer idUsuarioCierra;

    @Column(name = "cerrado_en")
    private Instant cerradoEn;
}
