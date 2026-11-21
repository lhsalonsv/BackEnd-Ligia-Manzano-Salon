package com.ligiamanzano.salon.personal.dto;

import jakarta.validation.constraints.*;

import java.time.LocalTime;

public record HorarioEmpleadoDTO(
        @NotNull @Min(1) @Max(7) Short diaSemana,
        @NotNull(message = "Escribe la hora de inicio") LocalTime horaInicio,
        @NotNull(message = "Escribe la hora de fin") LocalTime horaFin
) {}
