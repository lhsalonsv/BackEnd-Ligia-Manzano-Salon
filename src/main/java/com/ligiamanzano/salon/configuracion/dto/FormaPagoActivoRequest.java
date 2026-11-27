package com.ligiamanzano.salon.configuracion.dto;

import jakarta.validation.constraints.NotNull;

public record FormaPagoActivoRequest(
        @NotNull(message = "Falta la forma de pago") Short idFormaPago,
        @NotNull(message = "Indica si está activa") Boolean activo
) {}
