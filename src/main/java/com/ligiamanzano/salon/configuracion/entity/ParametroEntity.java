package com.ligiamanzano.salon.configuracion.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "tb_parametros")
@Getter
@Setter
public class ParametroEntity {

    @Id
    @Column(name = "clave", length = 40)
    private String clave;

    @Column(name = "valor", nullable = false, length = 100)
    private String valor;

    @Column(name = "descripcion", length = 200)
    private String descripcion;

    @Column(name = "actualizado_en", nullable = false, insertable = false, updatable = false)
    private Instant actualizadoEn;
}
