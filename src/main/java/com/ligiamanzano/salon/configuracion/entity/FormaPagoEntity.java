package com.ligiamanzano.salon.configuracion.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tb_formas_pago")
@Getter
@Setter
public class FormaPagoEntity {

    @Id
    private Short idFormaPago;
}
