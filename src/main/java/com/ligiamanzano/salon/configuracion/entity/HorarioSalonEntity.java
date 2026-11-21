package com.ligiamanzano.salon.configuracion.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tb_horarios")
@Getter
@Setter
public class HorarioSalonEntity {

    @Id
    private Short diaSemana;
}
