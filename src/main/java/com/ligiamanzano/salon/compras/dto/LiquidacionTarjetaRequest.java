package com.ligiamanzano.salon.compras.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LiquidacionTarjetaRequest(
        @NotNull(message = "Escribe la fecha") LocalDate fecha,
        @NotNull(message = "Elige el banco") Integer idProveedor,
        @NotBlank(message = "Escribe el número del documento") @Size(max = 40, message = "Hasta 40 caracteres") String numeroDocumento,
        @Pattern(regexp = "^[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}$", message = "El código de generación tiene 36 caracteres") String codigoGeneracion,
        @NotNull(message = "Elige el mes en que se declara") LocalDate periodoIva,
        @NotNull(message = "Escribe lo liquidado") @DecimalMin(value = "0.01", message = "Tiene que ser mayor que 0")
        @Digits(integer = 10, fraction = 2, message = "Hasta 2 decimales") BigDecimal montoLiquidado,
        @DecimalMin(value = "0.00", message = "No puede ser negativo") @Digits(integer = 10, fraction = 2, message = "Hasta 2 decimales") BigDecimal anticipoIva,
        @DecimalMin(value = "0.00", message = "No puede ser negativo") @Digits(integer = 10, fraction = 2, message = "Hasta 2 decimales") BigDecimal comisionTotal,
        @NotNull(message = "Escribe lo depositado") @DecimalMin(value = "0.00", message = "No puede ser negativo")
        @Digits(integer = 10, fraction = 2, message = "Hasta 2 decimales") BigDecimal montoDepositado,
        @NotBlank(message = "Elige la cuenta del banco") @Size(max = 12, message = "Hasta 12 caracteres") String codigoCuentaBanco,
        Integer idCompraComision
) {}
