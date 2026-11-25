package com.ligiamanzano.salon.compras.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tb_proveedores")
@Getter
@Setter
public class ProveedorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_proveedor")
    private Integer idProveedor;

    @Column(name = "razon_social", nullable = false, length = 250)
    private String razonSocial;

    @Column(name = "nombre_comercial", length = 150)
    private String nombreComercial;

    @Column(name = "nit", length = 14)
    private String nit;

    @Column(name = "nrc", length = 8)
    private String nrc;

    @Column(name = "nacionalidad", nullable = false, length = 40)
    private String nacionalidad;

    @Column(name = "contacto_nombre", length = 100)
    private String contactoNombre;

    @Column(name = "telefono", length = 30)
    private String telefono;

    @Column(name = "correo", length = 100)
    private String correo;

    @Column(name = "direccion", length = 200)
    private String direccion;

    @Column(name = "dias_credito", nullable = false)
    private Short diasCredito;

    @Column(name = "notas", length = 300)
    private String notas;

    @Column(name = "activo", nullable = false)
    private boolean activo;
}
