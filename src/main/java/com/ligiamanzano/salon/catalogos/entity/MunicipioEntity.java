package com.ligiamanzano.salon.catalogos.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "tb_cat_municipios")
@Getter
@Setter
public class MunicipioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_municipio")
    private Integer idMunicipio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cod_departamento", nullable = false)
    private DepartamentoEntity departamento;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "cod_municipio", nullable = false, length = 2)
    private String codMunicipio;

    @Column(name = "nombre", nullable = false, length = 80)
    private String nombre;
}
