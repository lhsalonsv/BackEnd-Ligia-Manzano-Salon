package com.ligiamanzano.salon.inventario.dto;

import java.math.BigDecimal;

public record ConteoProductoDTO(
        int idProducto,
        String codigo,
        String nombreProducto,
        String marca,
        String categoria,
        String unidadMedida,
        int existenciaSistema,
        BigDecimal valorSistema,
        Integer cantidadContada,
        String nota
) {}
