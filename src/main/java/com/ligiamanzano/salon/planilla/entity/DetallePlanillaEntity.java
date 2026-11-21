package com.ligiamanzano.salon.planilla.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tb_detalle_planillas")
@Getter
@Setter
public class DetallePlanillaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idDetallePlanilla;
}
