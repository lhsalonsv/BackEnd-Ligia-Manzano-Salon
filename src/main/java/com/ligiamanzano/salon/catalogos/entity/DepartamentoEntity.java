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
    @JdbcTypeCode(SqlTypes.CHAR)
    private String codDepartamento;
}
