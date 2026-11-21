package com.ligiamanzano.salon.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CambioContrasenaRequest(
        @NotBlank @Size(max = 64)
        String contrasenaActual,
        @NotBlank @Size(max = 64)
        String contrasenaNueva
) {}
