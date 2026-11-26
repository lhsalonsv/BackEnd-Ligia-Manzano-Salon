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
@Table(name = "tb_tipos_partida")
@Getter
@Setter
public class TipoPartidaEntity {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "codigo", length = 3)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 60)
    private String nombre;

    @Column(name = "predeterminado", nullable = false)
    private boolean predeterminado;

    @Column(name = "activo", nullable = false)
    private boolean activo;
}
