package com.ligiamanzano.salon.compras.dto;

import java.math.BigDecimal;

public record LineaCompraDTO(
        Integer idProducto,
        String nombreProducto,
        Integer cantidad,
        BigDecimal costoUnitario,
        BigDecimal subtotal,
        Integer cantidadDisponible
) {}
