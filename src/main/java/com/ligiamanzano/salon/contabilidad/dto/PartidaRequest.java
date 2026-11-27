package com.ligiamanzano.salon.contabilidad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record PartidaRequest(
        @NotNull(message = "Elige la fecha de la partida") LocalDate fecha,
        @NotBlank(message = "Elige un tipo de partida activo") String codigoTipo,
        @NotBlank(message = "Escribe el concepto de la partida") @Size(max = 300, message = "El concepto llega a 300 caracteres") String concepto,
        List<LineaPartidaRequest> lineas
) {}
