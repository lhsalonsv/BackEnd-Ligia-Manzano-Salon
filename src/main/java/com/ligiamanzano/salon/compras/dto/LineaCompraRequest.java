package com.ligiamanzano.salon.compras.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record LineaCompraRequest(
        Integer idProducto,
        @Size(max = 200, message = "La descripción admite hasta 200 caracteres") String descripcion,
        @NotNull(message = "Escribe la cantidad") @Min(value = 1, message = "La cantidad tiene que ser mayor que 0")
        @Max(value = 9999, message = "La cantidad máxima es 9999") Integer cantidad,
        @NotNull(message = "Escribe el costo") @DecimalMin(value = "0.0000", message = "El costo no puede ser negativo")
        @Digits(integer = 8, fraction = 4, message = "El costo admite hasta 4 decimales") BigDecimal costoUnitario
) {}
