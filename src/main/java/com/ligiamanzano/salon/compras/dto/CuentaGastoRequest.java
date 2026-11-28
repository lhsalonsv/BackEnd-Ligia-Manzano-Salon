package com.ligiamanzano.salon.compras.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CuentaGastoRequest(
        @NotNull(message = "Falta la línea de la compra") Integer idDetalleCompra,
        @Size(max = 12, message = "El código de la cuenta admite hasta 12 caracteres") String codigoCuenta
) {}
