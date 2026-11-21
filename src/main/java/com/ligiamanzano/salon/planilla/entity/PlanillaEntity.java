package com.ligiamanzano.salon.planilla.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tb_planillas")
@Getter
@Setter
public class PlanillaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idPlanilla;
}
