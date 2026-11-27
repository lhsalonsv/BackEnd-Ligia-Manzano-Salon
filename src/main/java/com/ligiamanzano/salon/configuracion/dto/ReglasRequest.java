package com.ligiamanzano.salon.configuracion.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record ReglasRequest(
        @NotNull(message = "Faltan los días del horario") @Size(min = 7, max = 7, message = "Faltan días del horario") List<@Valid HorarioSalonDTO> horarios,
        @NotNull(message = "Escribe el porcentaje") @DecimalMin(value = "0.00", message = "De 0 a 100") @DecimalMax(value = "100.00", message = "De 0 a 100") @Digits(integer = 3, fraction = 2, message = "Hasta 2 decimales") BigDecimal comisionServicioPct,
        @NotNull(message = "Escribe el monto") @DecimalMin(value = "0.00", message = "No puede ser negativo") @Digits(integer = 10, fraction = 2, message = "Hasta 2 decimales") BigDecimal comisionProducto,
        @NotNull(message = "Escribe el porcentaje") @DecimalMin(value = "0.00", message = "De 0 a 100") @DecimalMax(value = "100.00", message = "De 0 a 100") @Digits(integer = 3, fraction = 2, message = "Hasta 2 decimales") BigDecimal isssEmpleadoPct,
        @NotNull(message = "Escribe el monto") @DecimalMin(value = "0.00", message = "No puede ser negativo") @Digits(integer = 10, fraction = 2, message = "Hasta 2 decimales") BigDecimal isssTopeMensual,
        @NotNull(message = "Escribe el porcentaje") @DecimalMin(value = "0.00", message = "De 0 a 100") @DecimalMax(value = "100.00", message = "De 0 a 100") @Digits(integer = 3, fraction = 2, message = "Hasta 2 decimales") BigDecimal afpEmpleadoPct,
        @NotEmpty(message = "Faltan las formas de pago") List<@Valid FormaPagoActivoRequest> formasPago
) {}
