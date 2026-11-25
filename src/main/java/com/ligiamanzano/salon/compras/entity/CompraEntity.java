package com.ligiamanzano.salon.compras.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "tb_compras")
@Getter
@Setter
public class CompraEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_compra")
    private Integer idCompra;

    @Column(name = "numero_compra", nullable = false, unique = true, length = 10)
    private String numeroCompra;

    @Column(name = "id_proveedor", nullable = false)
    private Integer idProveedor;

    @Column(name = "fecha_compra", nullable = false)
    private LocalDate fechaCompra;

    @Column(name = "tipo_documento_proveedor", nullable = false, length = 10)
    private String tipoDocumentoProveedor;

    @Column(name = "numero_documento_proveedor", length = 40)
    private String numeroDocumentoProveedor;

    @Column(name = "id_compra_relacionada")
    private Integer idCompraRelacionada;

    @Column(name = "clase_documento", nullable = false)
    private Short claseDocumento;

    @Column(name = "codigo_generacion_proveedor")
    private UUID codigoGeneracionProveedor;

    @Column(name = "periodo_iva", nullable = false)
    private LocalDate periodoIva;

    @Column(name = "subtotal_sin_iva", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotalSinIva;

    @Column(name = "compras_exentas", nullable = false, precision = 12, scale = 2)
    private BigDecimal comprasExentas;

    @Column(name = "iva", nullable = false, precision = 12, scale = 2)
    private BigDecimal iva;

    @Column(name = "iva_percibido", nullable = false, precision = 12, scale = 2)
    private BigDecimal ivaPercibido;

    @Column(name = "total", nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(name = "tipo_operacion_renta", nullable = false)
    private Short tipoOperacionRenta;

    @Column(name = "clasificacion_renta", nullable = false)
    private Short clasificacionRenta;

    @Column(name = "sector_renta", nullable = false)
    private Short sectorRenta;

    @Column(name = "tipo_costo_gasto", nullable = false)
    private Short tipoCostoGasto;

    @Column(name = "fecha_vencimiento_pago")
    private LocalDate fechaVencimientoPago;

    @Column(name = "estado", nullable = false, length = 12)
    private String estado;

    @Column(name = "id_usuario", nullable = false)
    private Integer idUsuario;

    @Column(name = "creado_en", nullable = false, insertable = false, updatable = false)
    private Instant creadoEn;
}
