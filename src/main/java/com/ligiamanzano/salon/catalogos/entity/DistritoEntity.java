package com.ligiamanzano.salon.catalogos.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "tb_cat_distritos")
@Getter
@Setter
public class DistritoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_distrito")
    private Integer idDistrito;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_municipio", nullable = false)
    private MunicipioEntity municipio;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "cod_distrito", nullable = false, length = 2)
    private String codDistrito;

    @Column(name = "nombre", nullable = false, length = 80)
    private String nombre;
}
