package com.ligiamanzano.salon.inventario.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tb_inventario_fisico_conteo")
@Getter
@Setter
public class ConteoFisicoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_conteo")
    private Integer idConteo;

    @Column(name = "id_inventario_fisico", nullable = false)
    private Integer idInventarioFisico;

    @Column(name = "id_producto", nullable = false)
    private Integer idProducto;

    @Column(name = "cantidad_contada", nullable = false)
    private Integer cantidadContada;

    @Column(name = "existencia_sistema")
    private Integer existenciaSistema;

    @Column(name = "nota", length = 150)
    private String nota;
}
