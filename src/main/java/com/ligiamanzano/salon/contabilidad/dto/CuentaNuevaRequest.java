package com.ligiamanzano.salon.contabilidad.dto;

import jakarta.validation.constraints.*;

public record CuentaNuevaRequest(
        @NotBlank(message = "Escribe el código") @Pattern(regexp = "^[0-9]{1,12}$", message = "El código son solo números, hasta 12 dígitos") String codigo,
        @NotBlank(message = "Escribe el nombre de la cuenta") @Size(max = 150, message = "El nombre llega a 150 caracteres") String nombre,
        @NotBlank(message = "Elige el tipo") @Pattern(regexp = "^(ACTIVO|PASIVO|CAPITAL|INGRESO|COSTO_GASTO|LIQUIDADORA|ORDEN_DEUDORA|ORDEN_ACREEDORA)$", message = "Elige el tipo") String tipo,
        @NotBlank(message = "Elige la naturaleza") @Pattern(regexp = "^[DA]$", message = "Elige la naturaleza") String naturaleza,
        @NotNull(message = "Escribe el nivel") @Min(value = 0, message = "El nivel va de 0 a 9") @Max(value = 9, message = "El nivel va de 0 a 9") Short nivel,
        String codigoPadre,
        boolean aceptaMovimiento
) {}
