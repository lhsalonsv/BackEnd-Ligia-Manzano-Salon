package com.ligiamanzano.salon.clientes.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tb_formulas_color")
@Getter
@Setter
public class FormulaColorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idFormula;
}
