package com.ligiamanzano.salon.servicios.dto;

import java.math.BigDecimal;
import java.util.List;

public record ServicioDetalleDTO(
        Integer idServicio,
        String nombreServicio,
        String descripcion,
        Integer idEspecialidad,
        Short duracionMin,
        BigDecimal precio,
        boolean precioDesde,
        boolean activo,
        int vecesEsteMes,
        boolean tieneVentas,
        List<QuienRealizaDTO> quienes
) {}
