package com.ligiamanzano.salon.servicios.dto;

import java.math.BigDecimal;

public record ServicioDTO(
        Integer idServicio,
        String nombreServicio,
        String descripcion,
        Integer idEspecialidad,
        Short duracionMin,
        BigDecimal precio,
        boolean precioDesde,
        boolean activo
) {}
