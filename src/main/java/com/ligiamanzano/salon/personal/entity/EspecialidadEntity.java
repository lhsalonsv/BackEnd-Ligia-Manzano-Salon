package com.ligiamanzano.salon.personal.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tb_especialidades")
@Getter
@Setter
public class EspecialidadEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_especialidad")
    private Integer idEspecialidad;

    @Column(name = "nombre_especialidad", nullable = false, unique = true, length = 40)
    private String nombreEspecialidad;
}