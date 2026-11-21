package com.ligiamanzano.salon.inventario.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record LoteDTO(
        Integer idLote,
        OffsetDateTime fechaIngreso,
        BigDecimal costoUnitario,
        Integer cantidadInicial,
        Integer cantidadDisponible,
        String numeroCompra
) {}
