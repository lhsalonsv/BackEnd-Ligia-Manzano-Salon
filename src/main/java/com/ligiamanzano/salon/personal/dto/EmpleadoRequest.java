package com.ligiamanzano.salon.personal.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record EmpleadoRequest(
        @NotBlank(message = "Escribe el nombre") @Size(max = 80) String nombreEmpleado,
        @NotBlank(message = "Escribe el apellido") @Size(max = 80) String apellido,
        @NotBlank(message = "Escribe el DUI") @Pattern(regexp = "^\\d{8}-\\d$", message = "DUI con formato 00000000-0") String dui,
        @NotBlank(message = "Escribe el teléfono") @Pattern(regexp = "^(\\d{4}-\\d{4}|\\+\\d{8,15})$", message = "Usa 7000-0000 o +código y número") String telefono,
        @Email(message = "Correo no válido") @Size(max = 100) String correoEmpleado,
        @NotNull(message = "Escribe la fecha de ingreso") LocalDate fechaIngreso,
        @NotBlank(message = "Escribe el puesto") @Size(max = 40) String puesto,
        @NotNull(message = "Escribe el sueldo") @DecimalMin(value = "0.00", message = "No puede ser negativo") @Digits(integer = 10, fraction = 2) BigDecimal sueldoBaseMensual,
        boolean recibeComision,
        @Pattern(regexp = "^(CRECER|CONFIA)$", message = "Elige CRECER o CONFIA") String afpAdministradora,
        @Pattern(regexp = "^\\d{12}$", message = "NUP de 12 dígitos") String nupAfp,
        @Pattern(regexp = "^\\d{9}$", message = "ISSS de 9 dígitos") String numeroIsss,
        @NotNull List<Integer> especialidades,
        @NotNull List<@Valid HorarioEmpleadoDTO> horarios
) {}
