package com.ligiamanzano.salon.contabilidad.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "tb_cuentas_contables")
@Getter
@Setter
public class CuentaContableEntity {

    @Id
    @Column(name = "codigo", length = 12)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "tipo", nullable = false, length = 15)
    private String tipo;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "naturaleza", nullable = false, length = 1)
    private String naturaleza;

    @Column(name = "nivel", nullable = false)
    private Short nivel;

    @Column(name = "codigo_padre", length = 12)
    private String codigoPadre;

    @Column(name = "acepta_movimiento", nullable = false)
    private boolean aceptaMovimiento;

    @Column(name = "activa", nullable = false)
    private boolean activa;
}
