package com.ligiamanzano.salon.personal.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tb_empleado_especialidad")
@Getter
@Setter
public class EmpleadoEspecialidadEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_empleado_especialidad")
    private Integer idEmpleadoEspecialidad;

    @Column(name = "id_empleado", nullable = false)
    private Integer idEmpleado;

    @Column(name = "id_especialidad", nullable = false)
    private Integer idEspecialidad;
}