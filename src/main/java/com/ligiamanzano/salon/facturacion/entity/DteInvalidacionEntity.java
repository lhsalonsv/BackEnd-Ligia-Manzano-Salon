package com.ligiamanzano.salon.facturacion.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tb_dtes_invalidaciones")
@Getter
@Setter
public class DteInvalidacionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idDteInvalidacion;
}
