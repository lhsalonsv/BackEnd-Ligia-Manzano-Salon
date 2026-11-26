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

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;
}
