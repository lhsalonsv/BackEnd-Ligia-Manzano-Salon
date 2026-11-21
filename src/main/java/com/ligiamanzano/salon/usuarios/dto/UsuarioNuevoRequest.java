package com.ligiamanzano.salon.usuarios.dto;

import jakarta.validation.constraints.*;

public record UsuarioNuevoRequest(
        @NotBlank(message = "Escribe el usuario") @Size(min = 3, max = 50, message = "De 3 a 50 caracteres")
        @Pattern(regexp = "^[a-z0-9._-]+$", message = "Solo minúsculas, números, punto, guion y guion bajo") String nombreUsuario,
        @NotBlank(message = "Escribe el nombre completo") @Size(max = 150) String nombreCompleto,
        @NotBlank(message = "Elige el rol") @Pattern(regexp = "^(ADMINISTRADOR|RECEPCIONISTA|CONTADOR|AUDITOR)$", message = "Rol no válido") String rol,
        Integer idEmpleado
) {}
