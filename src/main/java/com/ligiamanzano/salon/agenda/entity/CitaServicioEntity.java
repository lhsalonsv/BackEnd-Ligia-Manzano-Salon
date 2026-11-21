package com.ligiamanzano.salon.agenda.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tb_cita_servicios")
@Getter
@Setter
public class CitaServicioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idCitaServicio;
}
