package com.ligiamanzano.salon.inventario.dto;

import java.time.OffsetDateTime;

public record MovimientoResumenDTO(
        OffsetDateTime fechaHora,
        String tipo,
        Integer cantidad,
        String referencia,
        String motivo
) {}
