package com.ligiamanzano.salon.clientes.dto;

import java.time.OffsetDateTime;

public record ProximaCitaDTO(OffsetDateTime fechaHoraInicio, String servicio, String empleado) {}
