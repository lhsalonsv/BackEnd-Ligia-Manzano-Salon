package com.ligiamanzano.salon.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Escribe tu usuario") @Size(max = 50) String nombreUsuario,
        @NotBlank(message = "Escribe tu contraseña") @Size(max = 64) String contrasena
) {}
