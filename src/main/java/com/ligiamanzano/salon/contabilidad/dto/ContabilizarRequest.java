package com.ligiamanzano.salon.contabilidad.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ContabilizarRequest(
        @NotNull(message = "Escribe la fecha inicial") LocalDate desde,
        @NotNull(message = "Escribe la fecha final") LocalDate hasta
) {}
