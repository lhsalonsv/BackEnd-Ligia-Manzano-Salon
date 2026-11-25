package com.ligiamanzano.salon.inventario.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "tb_productos")
@Getter
@Setter
public class ProductoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_producto")
    private Integer idProducto;

    @Column(name = "codigo", nullable = false, unique = true, length = 25)
    private String codigo;

    @Column(name = "nombre_producto", nullable = false, length = 100)
    private String nombreProducto;

    @Column(name = "descripcion", length = 300)
    private String descripcion;

    @Column(name = "id_marca", nullable = false)
    private Integer idMarca;

    @Column(name = "id_categoria", nullable = false)
    private Integer idCategoria;

    @Column(name = "cod_unidad_medida", nullable = false)
    private Short codUnidadMedida;

    @Column(name = "precio_venta", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioVenta;

    @Column(name = "comision_fija", nullable = false, precision = 12, scale = 2)
    private BigDecimal comisionFija;

    @Column(name = "stock_minimo", nullable = false)
    private Integer stockMinimo;

    @Column(name = "activo", nullable = false)
    private boolean activo;
}