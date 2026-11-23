package com.ligiamanzano.salon.configuracion.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Entity
@Table(name = "tb_horarios")
@Getter
@Setter
public class HorarioSalonEntity {

    @Id
    @Column(name = "dia_semana")
    private Short diaSemana;

    @Column(name = "abierto", nullable = false)
    private boolean abierto;

    @Column(name = "hora_abre")
    private LocalTime horaAbre;

    @Column(name = "hora_cierra")
    private LocalTime horaCierra;
}