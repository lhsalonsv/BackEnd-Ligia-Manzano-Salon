package com.ligiamanzano.salon.usuarios.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tb_roles")
@Getter
@Setter
public class RolEntity {

    @Id
    @Column(name = "id_rol")
    private Short idRol;

    @Column(name = "nombre_rol", nullable = false, unique = true, length = 30)
    private String nombreRol;

    @Column(name = "descripcion", length = 200)
    private String descripcion;
}
