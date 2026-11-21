package com.ligiamanzano.salon.inventario.dto;

import java.math.BigDecimal;

public record ProductoListaDTO(
        Integer idProducto,
        String codigo,
        String nombreProducto,
        String nombreCategoria,
        int existencia,
        Integer stockMinimo,
        BigDecimal costoPeps,
        BigDecimal precioVenta,
        BigDecimal valorCosto,
        boolean activo
) {}
