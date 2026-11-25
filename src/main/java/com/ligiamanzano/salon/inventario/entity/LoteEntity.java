package com.ligiamanzano.salon.inventario.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "tb_lotes")
@Getter
@Setter
public class LoteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_lote")
    private Integer idLote;

    @Column(name = "id_producto", nullable = false)
    private Integer idProducto;

    @Column(name = "id_detalle_compra", unique = true)
    private Integer idDetalleCompra;

    @Column(name = "fecha_ingreso", nullable = false, insertable = false, updatable = false)
    private Instant fechaIngreso;

    @Column(name = "costo_unitario", nullable = false, precision = 12, scale = 4)
    private BigDecimal costoUnitario;

    @Column(name = "cantidad_inicial", nullable = false)
    private Integer cantidadInicial;

    @Column(name = "cantidad_disponible", nullable = false, insertable = false, updatable = false)
    private Integer cantidadDisponible;
}