package com.ligiamanzano.salon.inventario.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicInsert;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@DynamicInsert
@Table(name = "tb_movimientos_inventario")
@Getter
@Setter
public class MovimientoInventarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_movimiento")
    private Long idMovimiento;

    @Column(name = "id_lote", nullable = false)
    private Integer idLote;

    @Column(name = "id_producto", nullable = false)
    private Integer idProducto;

    @Column(name = "tipo", nullable = false, length = 20)
    private String tipo;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    @Column(name = "costo_unitario", nullable = false, precision = 12, scale = 4)
    private BigDecimal costoUnitario;

    @Column(name = "id_detalle_compra")
    private Integer idDetalleCompra;

    @Column(name = "id_detalle_venta")
    private Integer idDetalleVenta;

    @Column(name = "id_nc_detalle")
    private Integer idNcDetalle;

    @Column(name = "id_inventario_fisico")
    private Integer idInventarioFisico;

    @Column(name = "motivo", length = 150)
    private String motivo;

    @Column(name = "id_usuario", nullable = false)
    private Integer idUsuario;

    @Column(name = "fecha_hora", nullable = false, updatable = false)
    private Instant fechaHora;
}
