package com.ligiamanzano.salon.configuracion.dto;

import jakarta.validation.constraints.NotNull;

public record FormaPagoActivoRequest(@NotNull Short idFormaPago, @NotNull Boolean activo) {}
