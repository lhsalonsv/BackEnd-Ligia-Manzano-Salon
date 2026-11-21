package com.ligiamanzano.salon.configuracion.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record ReglasRequest(
        @NotNull @Size(min = 7, max = 7, message = "Faltan días del horario") List<@Valid HorarioSalonDTO> horarios,
        @NotNull @DecimalMin("0.00") @DecimalMax(value = "100.00", message = "De 0 a 100") @Digits(integer = 3, fraction = 2) BigDecimal comisionServicioPct,
        @NotNull @DecimalMin(value = "0.00", message = "No puede ser negativo") @Digits(integer = 10, fraction = 2) BigDecimal comisionProducto,
        @NotNull @DecimalMin("0.00") @DecimalMax(value = "100.00", message = "De 0 a 100") @Digits(integer = 3, fraction = 2) BigDecimal isssEmpleadoPct,
        @NotNull @DecimalMin(value = "0.00", message = "No puede ser negativo") @Digits(integer = 10, fraction = 2) BigDecimal isssTopeMensual,
        @NotNull @DecimalMin("0.00") @DecimalMax(value = "100.00", message = "De 0 a 100") @Digits(integer = 3, fraction = 2) BigDecimal afpEmpleadoPct,
        @NotEmpty List<@Valid FormaPagoActivoRequest> formasPago
) {}
