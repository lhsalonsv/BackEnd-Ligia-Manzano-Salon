package com.ligiamanzano.salon.personal.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record BajaRequest(@NotNull(message = "Escribe la fecha de baja") LocalDate fechaBaja) {}
