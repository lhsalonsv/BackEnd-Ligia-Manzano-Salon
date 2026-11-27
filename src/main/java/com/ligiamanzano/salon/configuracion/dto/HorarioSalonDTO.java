package com.ligiamanzano.salon.configuracion.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;

import java.time.LocalTime;

public record HorarioSalonDTO(
        @NotNull(message = "Falta el día") @Min(value = 1, message = "Día de 1 (lunes) a 7 (domingo)") @Max(value = 7, message = "Día de 1 (lunes) a 7 (domingo)")
        Short diaSemana,
        boolean abierto,
        @JsonFormat(pattern = "HH:mm")
        LocalTime horaAbre,
        @JsonFormat(pattern = "HH:mm")
        LocalTime horaCierra
) {}
