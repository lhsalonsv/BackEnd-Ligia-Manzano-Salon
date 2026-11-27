package com.ligiamanzano.salon.contabilidad.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record AnulacionRequest(
        @Size(max = 250, message = "El motivo llega a 250 caracteres") String motivo,
        @NotNull(message = "Elige la fecha de la anulación") LocalDate fecha
) {}
