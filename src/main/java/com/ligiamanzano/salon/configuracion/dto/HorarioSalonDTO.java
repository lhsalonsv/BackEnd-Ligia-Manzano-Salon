package com.ligiamanzano.salon.configuracion.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;

import java.time.LocalTime;

public record HorarioSalonDTO(
        @NotNull @Min(1) @Max(7)
        Short diaSemana,
        boolean abierto,
        @JsonFormat(pattern = "HH:mm")
        LocalTime horaAbre,
        @JsonFormat(pattern = "HH:mm")
        LocalTime horaCierra
) {}
