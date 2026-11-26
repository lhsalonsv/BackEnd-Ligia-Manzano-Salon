package com.ligiamanzano.salon.contabilidad.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "tb_partida_detalle")
@Getter
@Setter
public class PartidaDetalleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle")
    private Long idDetalle;

    @Column(name = "id_partida", nullable = false)
    private Integer idPartida;

    @Column(name = "linea", nullable = false)
    private Short linea;

    @Column(name = "codigo_cuenta", nullable = false, length = 12)
    private String codigoCuenta;

    @Column(name = "concepto", length = 300)
    private String concepto;

    @Column(name = "debe", nullable = false, precision = 14, scale = 2)
    private BigDecimal debe;

    @Column(name = "haber", nullable = false, precision = 14, scale = 2)
    private BigDecimal haber;
}
