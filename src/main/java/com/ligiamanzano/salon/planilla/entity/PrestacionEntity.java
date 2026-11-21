package com.ligiamanzano.salon.planilla.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tb_prestaciones")
@Getter
@Setter
public class PrestacionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idPrestacion;
}
