package com.ligiamanzano.salon.inventario.dto;

import java.math.BigDecimal;

public record ActaFilaDTO(
        String categoria,
        String codigo,
        String nombreProducto,
        String nombreMarca,
        String unidadMedida,
        int idLote,
        long cantidad,
        BigDecimal costoUnitario,
        BigDecimal valor,
        String referenciaCosto
) {}
