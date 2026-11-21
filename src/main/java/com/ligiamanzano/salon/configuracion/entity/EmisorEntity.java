package com.ligiamanzano.salon.configuracion.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tb_emisores")
@Getter
@Setter
public class EmisorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idEmisor;
}
