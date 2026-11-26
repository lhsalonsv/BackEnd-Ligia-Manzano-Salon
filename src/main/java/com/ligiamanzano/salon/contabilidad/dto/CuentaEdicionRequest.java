package com.ligiamanzano.salon.contabilidad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CuentaEdicionRequest(
        @NotBlank(message = "Escribe el nombre de la cuenta") @Size(max = 150, message = "El nombre llega a 150 caracteres") String nombre,
        boolean aceptaMovimiento,
        boolean activa
) {}
