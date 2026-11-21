package com.ligiamanzano.salon.facturacion.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tb_nc_detalle")
@Getter
@Setter
public class NcDetalleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idNcDetalle;
}
