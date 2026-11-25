package com.ligiamanzano.salon.inventario.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ProductoRequest(
        @NotBlank(message = "Escribe el nombre")
        @Size(max = 100)
        String nombreProducto,
        @NotBlank(message = "Escribe el código")
        @Size(max = 25)
        @Pattern(regexp = "^[A-Za-z0-9-]+$", message = "Solo letras, números y guiones")
        String codigo,
        @Size(max = 300)
        String descripcion,
        @NotNull(message = "Elige la marca")
        Integer idMarca,
        @NotNull(message = "Elige la categoría")
        Integer idCategoria,
        @NotNull(message = "Elige la unidad de medida")
        Short codUnidadMedida,
        @NotNull(message = "Escribe el precio")
        @DecimalMin(value = "0.00", message = "No puede ser negativo")
        @Digits(integer = 10, fraction = 2)
        BigDecimal precioVenta,
        @NotNull(message = "Escribe la comisión")
        @DecimalMin(value = "0.00", message = "No puede ser negativo")
        @Digits(integer = 10, fraction = 2)
        BigDecimal comisionFija,
        @NotNull(message = "Escribe el mínimo")
        @Min(value = 0, message = "No puede ser negativo")
        Integer stockMinimo,
        boolean activo
) {}
