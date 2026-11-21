package com.ligiamanzano.salon.security;

import java.util.UUID;

public record UsuarioAutenticado(
        int idUsuario,
        String nombreUsuario,
        String nombreCompleto,
        Rol rol,
        boolean debeCambiarContrasena,
        UUID idSesion
) {}
