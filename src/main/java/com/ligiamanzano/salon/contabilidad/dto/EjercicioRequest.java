package com.ligiamanzano.salon.contabilidad.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record EjercicioRequest(
        @NotNull(message = "Escribe el año") @Min(value = 2000, message = "El año va de 2000 a 2100") @Max(value = 2100, message = "El año va de 2000 a 2100") Short anio
) {}
