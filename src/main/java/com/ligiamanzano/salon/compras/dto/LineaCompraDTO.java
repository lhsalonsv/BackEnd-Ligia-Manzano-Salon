package com.ligiamanzano.salon.compras.dto;

import java.math.BigDecimal;

public record LineaCompraDTO(
        Integer idDetalleCompra,
        Integer idProducto,
        String nombreProducto,
        String descripcion,
        Integer cantidad,
        BigDecimal costoUnitario,
        BigDecimal subtotal,
        Integer cantidadDisponible,
        String codigoCuenta
) {}
