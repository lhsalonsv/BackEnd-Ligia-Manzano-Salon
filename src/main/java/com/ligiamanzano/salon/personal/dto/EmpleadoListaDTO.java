package com.ligiamanzano.salon.personal.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EmpleadoListaDTO(
        Integer idEmpleado,
        String nombreEmpleado,
        String apellido,
        String puesto,
        String dui,
        String rol,
        BigDecimal sueldoBaseMensual,
        boolean recibeComision,
        BigDecimal comisionesQuincena,
        boolean activo,
        LocalDate fechaBaja
) {}
