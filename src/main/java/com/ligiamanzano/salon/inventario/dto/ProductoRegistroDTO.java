package com.ligiamanzano.salon.inventario.dto;

import java.util.List;

public record ProductoRegistroDTO(
        int idProducto,
        String codigo,
        String nombreProducto,
        String marca,
        String unidadMedida,
        SaldoDTO saldoInicial,
        List<MovimientoRegistroDTO> movimientos
) {}
