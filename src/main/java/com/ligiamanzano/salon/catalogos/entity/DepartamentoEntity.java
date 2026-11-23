package com.ligiamanzano.salon.catalogos.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "tb_cat_departamentos")
@Getter
@Setter
public class DepartamentoEntity {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR) //indica explícitamente al ORM que mapee un atributo de Java a una columna de tipo CHAR
    @Column(name = "cod_departamento", length = 2)
    private String codDepartamento;

    @Column(name = "nombre", nullable = false, length = 60)
    private String nombre;
}
