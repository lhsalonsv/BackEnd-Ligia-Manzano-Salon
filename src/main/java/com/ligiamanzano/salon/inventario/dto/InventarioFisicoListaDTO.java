package com.ligiamanzano.salon.inventario.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record InventarioFisicoListaDTO(
        int idInventarioFisico,
        LocalDate fechaCorte,
        String estado,
        long productosContados,
        OffsetDateTime creadoEn,
        OffsetDateTime cerradoEn
) {}
