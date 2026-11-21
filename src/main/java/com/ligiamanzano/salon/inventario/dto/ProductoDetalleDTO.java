package com.ligiamanzano.salon.inventario.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProductoDetalleDTO(
        Integer idProducto,
        String codigo,
        String nombreProducto,
        String descripcion,
        Integer idMarca,
        Integer idCategoria,
        Short codUnidadMedida,
        BigDecimal precioVenta,
        BigDecimal comisionFija,
        Integer stockMinimo,
        boolean activo,
        String nombreMarca,
        String nombreCategoria,
        int existencia,
        BigDecimal costoPeps,
        BigDecimal ultimoCosto,
        int vendidos30d,
        List<LoteDTO> lotes,
        List<MovimientoResumenDTO> ultimosMovimientos
) {}
