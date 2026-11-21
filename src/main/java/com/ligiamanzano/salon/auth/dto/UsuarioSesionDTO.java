package com.ligiamanzano.salon.auth.dto;

public record UsuarioSesionDTO(
        int idUsuario,
        String nombreUsuario,
        String nombreCompleto,
        String rol,
        boolean debeCambiarContrasena
) {}
