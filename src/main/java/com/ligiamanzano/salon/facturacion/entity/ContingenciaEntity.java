package com.ligiamanzano.salon.facturacion.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tb_contingencias")
@Getter
@Setter
public class ContingenciaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idContingencia;
}
