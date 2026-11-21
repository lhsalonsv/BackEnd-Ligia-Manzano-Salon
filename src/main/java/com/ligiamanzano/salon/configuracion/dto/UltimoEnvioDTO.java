package com.ligiamanzano.salon.configuracion.dto;

import java.time.OffsetDateTime;

public record UltimoEnvioDTO(OffsetDateTime fechaHora, String estado, String numeroControl) {}
