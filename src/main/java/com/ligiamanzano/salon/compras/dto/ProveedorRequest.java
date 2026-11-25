package com.ligiamanzano.salon.compras.dto;

import jakarta.validation.constraints.*;

public record ProveedorRequest(
        @NotBlank(message = "Escribe el nombre comercial") @Size(max = 150) String nombreComercial,
        @NotBlank(message = "Escribe la razón social") @Size(max = 250) String razonSocial,
        @NotBlank(message = "Escribe el NIT") @Pattern(regexp = "^(\\d{14}|\\d{9})$", message = "NIT de 14 dígitos (o 9 si es DUI homologado)") String nit,
        @NotBlank(message = "Escribe el NRC") @Pattern(regexp = "^\\d{2,8}$", message = "NRC de 2 a 8 dígitos") String nrc,
        @NotBlank(message = "Escribe el país del proveedor (El Salvador si es local)") @Size(max = 40, message = "Hasta 40 caracteres") String nacionalidad,
        @Size(max = 100) String contactoNombre,
        @Size(max = 30) String telefono,
        @Email(message = "Correo no válido") @Size(max = 100) String correo,
        @Size(max = 200) String direccion,
        @NotNull(message = "Escribe los días de crédito") @Min(value = 0, message = "De 0 a 120 días") @Max(value = 120, message = "De 0 a 120 días") Short diasCredito,
        @Size(max = 300) String notas
) {}
