package com.ligiamanzano.salon.personal.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Entity
@Table(name = "tb_horarios_empleado")
@Getter
@Setter
public class HorarioEmpleadoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_horario_empleado")
    private Integer idHorarioEmpleado;

    @Column(name = "id_empleado", nullable = false)
    private Integer idEmpleado;

    @Column(name = "dia_semana", nullable = false)
    private Short diaSemana;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;
}