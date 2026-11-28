package com.ligiamanzano.salon.compras.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "tb_liquidaciones_tarjeta")
@Getter
@Setter
public class LiquidacionTarjetaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_liquidacion")
    private Integer idLiquidacion;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "id_proveedor", nullable = false)
    private Integer idProveedor;

    @Column(name = "numero_documento", nullable = false, length = 40)
    private String numeroDocumento;

    @Column(name = "codigo_generacion")
    private UUID codigoGeneracion;

    @Column(name = "periodo_iva", nullable = false)
    private LocalDate periodoIva;

    @Column(name = "monto_liquidado", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoLiquidado;

    @Column(name = "anticipo_iva", nullable = false, precision = 12, scale = 2)
    private BigDecimal anticipoIva;

    @Column(name = "comision_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal comisionTotal;

    @Column(name = "monto_depositado", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoDepositado;

    @Column(name = "codigo_cuenta_banco", nullable = false, length = 12)
    private String codigoCuentaBanco;

    @Column(name = "id_compra_comision")
    private Integer idCompraComision;

    @Column(name = "id_usuario", nullable = false)
    private Integer idUsuario;

    @Column(name = "creado_en", nullable = false, insertable = false, updatable = false)
    private Instant creadoEn;
}
