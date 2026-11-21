package com.ligiamanzano.salon.catalogos.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tb_cat_unidades_medida")
@Getter
@Setter
public class UnidadMedidaEntity {

    @Id
    private Short codUnidadMedida;
}
