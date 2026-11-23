package com.ligiamanzano.salon.catalogos.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tb_cat_actividades")
@Getter
@Setter
public class ActividadEntity {

    @Id
    @Column(name = "cod_actividad", length = 6)
    private String codActividad;

    @Column(name = "descripcion", nullable = false, length = 150)
    private String descripcion;
}
