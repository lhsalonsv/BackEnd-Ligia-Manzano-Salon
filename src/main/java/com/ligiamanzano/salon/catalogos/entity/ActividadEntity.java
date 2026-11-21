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
    private String codActividad;
}
