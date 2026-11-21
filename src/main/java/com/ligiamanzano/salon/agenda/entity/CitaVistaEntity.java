package com.ligiamanzano.salon.agenda.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "v_citas")
@Immutable
@Getter
@Setter
public class CitaVistaEntity {

    @Id
    private Integer idCita;
}
