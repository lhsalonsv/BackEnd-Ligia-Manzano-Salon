package com.ligiamanzano.salon.configuracion.dto;

import jakarta.validation.constraints.*;

import java.time.LocalTime;

public record HorarioSalonDTO(
        @NotNull @Min(1) @Max(7) Short diaSemana,
        boolean abierto,
        LocalTime horaAbre,
        LocalTime horaCierra
) {}
