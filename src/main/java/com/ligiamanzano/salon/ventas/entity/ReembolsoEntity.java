package com.ligiamanzano.salon.ventas.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tb_reembolsos")
@Getter
@Setter
public class ReembolsoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idReembolso;
}
