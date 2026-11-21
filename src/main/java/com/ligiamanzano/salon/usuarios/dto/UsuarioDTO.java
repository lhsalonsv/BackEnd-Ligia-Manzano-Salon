package com.ligiamanzano.salon.usuarios.dto;

import java.time.OffsetDateTime;

public record UsuarioDTO(
        Integer idUsuario,
        String nombreUsuario,
        String nombreCompleto,
        String rol,
        boolean activo,
        boolean debeCambiarContrasena,
        OffsetDateTime ultimoAcceso,
        Integer idEmpleado
) {}
