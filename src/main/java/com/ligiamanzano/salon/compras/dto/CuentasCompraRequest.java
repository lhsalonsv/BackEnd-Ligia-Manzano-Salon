package com.ligiamanzano.salon.compras.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CuentasCompraRequest(
        List<@Valid CuentaGastoRequest> detalle,
        @Size(max = 12, message = "Hasta 12 caracteres") String codigoCuentaExentas
) {}
