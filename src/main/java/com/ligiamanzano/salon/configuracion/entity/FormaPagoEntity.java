package com.ligiamanzano.salon.configuracion.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "tb_formas_pago")
@Getter
@Setter
public class FormaPagoEntity {

    public static final String EFECTIVO = "01";

    @Id
    @Column(name = "id_forma_pago")
    private Short idFormaPago;

    @Column(name = "nombre", nullable = false, length = 40)
    private String nombre;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "cod_forma_pago_mh", nullable = false, length = 2, updatable = false)
    private String codFormaPagoMh;

    @Column(name = "pide_referencia", nullable = false)
    private boolean pideReferencia;

    @Column(name = "activo", nullable = false)
    private boolean activo;

    public boolean esEfectivo() {
        return EFECTIVO.equals(codFormaPagoMh);
    }
}
