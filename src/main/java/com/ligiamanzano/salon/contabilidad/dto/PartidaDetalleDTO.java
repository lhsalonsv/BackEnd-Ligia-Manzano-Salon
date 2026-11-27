package com.ligiamanzano.salon.contabilidad.dto;

import com.ligiamanzano.salon.configuracion.dto.EmisorEncabezadoDTO;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import java.util.List;

public record PartidaDetalleDTO(
        @JsonUnwrapped PartidaListaDTO partida,
        String nombreTipo,
        List<LineaPartidaDTO> lineas,
        EmisorEncabezadoDTO emisor
) {}
