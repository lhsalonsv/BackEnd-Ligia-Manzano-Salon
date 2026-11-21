package com.ligiamanzano.salon.compras.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record LineaCompraRequest(
        @NotNull(message = "Elige el producto") Integer idProducto,
        @NotNull @Min(value = 1, message = "La cantidad tiene que ser mayor que 0") @Max(9999) Integer cantidad,
        @NotNull(message = "Escribe el costo") @DecimalMin(value = "0.0000", message = "No puede ser negativo")
        @Digits(integer = 8, fraction = 4) BigDecimal costoUnitario
) {}
