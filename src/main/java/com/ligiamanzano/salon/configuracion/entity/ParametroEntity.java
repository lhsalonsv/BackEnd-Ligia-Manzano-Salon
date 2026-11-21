package com.ligiamanzano.salon.configuracion.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tb_parametros")
@Getter
@Setter
public class ParametroEntity {

    @Id
    private String clave;
}
