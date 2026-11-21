package com.ligiamanzano.salon.usuarios.dto;

import jakarta.validation.constraints.*;

public record UsuarioEdicionRequest(
        @NotBlank(message = "Escribe el nombre completo") @Size(max = 150) String nombreCompleto,
        @NotBlank(message = "Elige el rol") @Pattern(regexp = "^(ADMINISTRADOR|RECEPCIONISTA|CONTADOR|AUDITOR)$", message = "Rol no válido") String rol,
        Integer idEmpleado
) {}
