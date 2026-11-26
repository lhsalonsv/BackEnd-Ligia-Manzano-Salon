package com.ligiamanzano.salon.inventario.dto;

import com.ligiamanzano.salon.configuracion.dto.EmisorEncabezadoDTO;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record InventarioFisicoDTO(
        int idInventarioFisico,
        LocalDate fechaCorte,
        String estado,
        OffsetDateTime cerradoEn,
        String observaciones,
        EmisorEncabezadoDTO emisor,
        List<ConteoProductoDTO> conteo,
        List<ActaFilaDTO> acta,
        List<AjusteFisicoDTO> ajustes
) {}
