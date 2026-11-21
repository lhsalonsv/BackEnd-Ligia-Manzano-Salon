package com.ligiamanzano.salon.servicios.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ServicioRequest(
        @NotBlank(message = "Escribe el nombre") @Size(max = 80) String nombreServicio,
        @NotNull(message = "Elige la especialidad") Integer idEspecialidad,
        @Size(max = 300) String descripcion,
        @Min(value = 5, message = "Mínimo 5 minutos") @Max(value = 600, message = "Máximo 600 minutos") Short duracionMin,
        @NotNull(message = "Escribe el precio")
        @DecimalMin("0.00") @DecimalMax("9999.99") @Digits(integer = 4, fraction = 2) BigDecimal precio,
        boolean precioDesde
) {}
