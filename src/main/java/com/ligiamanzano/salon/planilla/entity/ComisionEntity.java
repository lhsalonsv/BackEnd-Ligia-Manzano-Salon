package com.ligiamanzano.salon.planilla.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tb_comisiones")
@Getter
@Setter
public class ComisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idComision;
}
