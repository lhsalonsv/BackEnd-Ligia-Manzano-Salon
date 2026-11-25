package com.ligiamanzano.salon.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class UsuarioActual {

    private UsuarioActual() {}

    public static int id() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UsuarioAutenticado u) {
            return u.idUsuario();
        }
        throw new IllegalStateException("Esta operación necesita un usuario con sesión");
    }
}