package com.ligiamanzano.salon.personal.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "tb_empleados")
@Getter
@Setter
public class EmpleadoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_empleado")
    private Integer idEmpleado;

    @Column(name = "nombre_empleado", nullable = false, length = 80)
    private String nombreEmpleado;

    @Column(name = "apellido", nullable = false, length = 80)
    private String apellido;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "dui", unique = true, length = 10)
    private String dui;

    @Column(name = "telefono", length = 30)
    private String telefono;

    @Column(name = "correo_empleado", length = 100)
    private String correoEmpleado;

    @Column(name = "puesto", nullable = false, length = 40)
    private String puesto;

    @Column(name = "fecha_ingreso", nullable = false)
    private LocalDate fechaIngreso;

    @Column(name = "fecha_baja")
    private LocalDate fechaBaja;

    @Column(name = "sueldo_base_mensual", nullable = false, precision = 12, scale = 2)
    private BigDecimal sueldoBaseMensual;

    @Column(name = "recibe_comision", nullable = false)
    private boolean recibeComision;

    @Column(name = "nup_afp", length = 12)
    private String nupAfp;

    @Column(name = "afp_administradora", length = 6)
    private String afpAdministradora;

    @Column(name = "numero_isss", length = 9)
    private String numeroIsss;

    @Column(name = "id_usuario", unique = true)
    private Integer idUsuario;

    @Column(name = "activo", nullable = false)
    private boolean activo;
}