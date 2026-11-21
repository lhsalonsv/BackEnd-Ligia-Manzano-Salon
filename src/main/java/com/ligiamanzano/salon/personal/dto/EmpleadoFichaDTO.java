package com.ligiamanzano.salon.personal.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record EmpleadoFichaDTO(
        Integer idEmpleado,
        String nombreEmpleado,
        String apellido,
        String dui,
        String telefono,
        String correoEmpleado,
        String puesto,
        LocalDate fechaIngreso,
        LocalDate fechaBaja,
        BigDecimal sueldoBaseMensual,
        boolean recibeComision,
        String nupAfp,
        String afpAdministradora,
        String numeroIsss,
        Integer idUsuario,
        boolean activo,
        List<Integer> especialidades,
        List<HorarioEmpleadoDTO> horarios,
        UsuarioLigadoDTO usuario
) {}
