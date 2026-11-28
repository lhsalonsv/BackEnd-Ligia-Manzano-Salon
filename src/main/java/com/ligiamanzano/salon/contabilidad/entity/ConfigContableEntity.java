package com.ligiamanzano.salon.contabilidad.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "tb_config_contable")
@Getter
@Setter
public class ConfigContableEntity {

    public static final short UNICA = 1;

    @Id
    @Column(name = "id")
    private Short id;

    @Column(name = "cta_utilidad_ejercicio", length = 12)
    private String ctaUtilidadEjercicio;

    @Column(name = "cta_utilidades_anteriores", length = 12)
    private String ctaUtilidadesAnteriores;

    @Column(name = "cta_perdida_ejercicio", length = 12)
    private String ctaPerdidaEjercicio;

    @Column(name = "cta_perdidas_anteriores", length = 12)
    private String ctaPerdidasAnteriores;

    @Column(name = "cta_perdidas_ganancias", length = 12)
    private String ctaPerdidasGanancias;

    @Column(name = "cta_reserva_legal", length = 12)
    private String ctaReservaLegal;

    @Column(name = "cta_clientes", length = 12)
    private String ctaClientes;

    @Column(name = "cta_proveedores", length = 12)
    private String ctaProveedores;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "tipo_partida_ventas", nullable = false, length = 3)
    private String tipoPartidaVentas;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "tipo_partida_compras", nullable = false, length = 3)
    private String tipoPartidaCompras;

    @Column(name = "modo_ventas", nullable = false, length = 8)
    private String modoVentas;

    @Column(name = "modo_compras", nullable = false, length = 8)
    private String modoCompras;

    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    @Column(name = "cta_caja", length = 12)
    private String ctaCaja;

    @Column(name = "cta_ingreso_ventas", length = 12)
    private String ctaIngresoVentas;

    @Column(name = "cta_compras_productos", length = 12)
    private String ctaComprasProductos;

    @Column(name = "cta_iva_debito_ccf", length = 12)
    private String ctaIvaDebitoCcf;

    @Column(name = "cta_iva_debito_cf", length = 12)
    private String ctaIvaDebitoCf;

    @Column(name = "cta_iva_credito", length = 12)
    private String ctaIvaCredito;

    @Column(name = "cta_iva_percibido", length = 12)
    private String ctaIvaPercibido;

    @Column(name = "cta_iva_retenido", length = 12)
    private String ctaIvaRetenido;

    @Column(name = "cta_anticipo_iva", length = 12)
    private String ctaAnticipoIva;

    @Column(name = "cta_remanente_iva", length = 12)
    private String ctaRemanenteIva;

    @Column(name = "cta_iva_por_pagar", length = 12)
    private String ctaIvaPorPagar;

    @Column(name = "cta_pago_cuenta", length = 12)
    private String ctaPagoCuenta;

    @Column(name = "cta_pago_cuenta_por_pagar", length = 12)
    private String ctaPagoCuentaPorPagar;

    @Column(name = "cta_sueldos", length = 12)
    private String ctaSueldos;

    @Column(name = "cta_comisiones", length = 12)
    private String ctaComisiones;

    @Column(name = "cta_isss_laboral", length = 12)
    private String ctaIsssLaboral;

    @Column(name = "cta_afp_laboral", length = 12)
    private String ctaAfpLaboral;

    @Column(name = "cta_renta_retenida", length = 12)
    private String ctaRentaRetenida;

    @Column(name = "cta_otros_descuentos", length = 12)
    private String ctaOtrosDescuentos;

    @Column(name = "cta_salarios_por_pagar", length = 12)
    private String ctaSalariosPorPagar;

    @Column(name = "cta_capital_social", length = 12)
    private String ctaCapitalSocial;

    @Column(name = "cta_gasto_isr", length = 12)
    private String ctaGastoIsr;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;
}
