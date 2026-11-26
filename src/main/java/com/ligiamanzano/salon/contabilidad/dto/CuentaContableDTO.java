package com.ligiamanzano.salon.contabilidad.dto;

public record CuentaContableDTO(
        String codigo,
        String nombre,
        String tipo,
        String naturaleza,
        short nivel,
        String codigoPadre,
        boolean aceptaMovimiento,
        boolean activa
) {}
