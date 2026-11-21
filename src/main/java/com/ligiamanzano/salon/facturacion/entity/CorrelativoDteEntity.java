package com.ligiamanzano.salon.facturacion.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tb_correlativos_dte")
@Getter
@Setter
public class CorrelativoDteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idCorrelativoDte;
}
