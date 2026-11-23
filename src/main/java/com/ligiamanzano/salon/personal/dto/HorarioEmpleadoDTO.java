package com.ligiamanzano.salon.personal.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;

import java.time.LocalTime;

public record HorarioEmpleadoDTO(
        @NotNull @Min(1) @Max(7)
        Short diaSemana,
        @NotNull(message = "Escribe la hora de inicio")
        @JsonFormat(pattern = "HH:mm")
        LocalTime horaInicio,
        @NotNull(message = "Escribe la hora de fin")
        @JsonFormat(pattern = "HH:mm")
        LocalTime horaFin
) {}
