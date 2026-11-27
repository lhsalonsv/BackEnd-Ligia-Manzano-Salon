package com.ligiamanzano.salon.configuracion.dto;

import jakarta.validation.constraints.*;

public record NegocioRequest(
        @Size(max = 150, message = "Hasta 150 caracteres") String nombreComercial,
        @NotBlank(message = "Escribe el nombre o razón social") @Size(max = 250, message = "Hasta 250 caracteres") String nombre,
        @NotBlank(message = "Escribe el NIT") @Pattern(regexp = "^(\\d{14}|\\d{9})$", message = "NIT de 14 dígitos (o 9 si es DUI homologado)") String nit,
        @NotBlank(message = "Escribe el NRC") @Pattern(regexp = "^\\d{2,8}$", message = "NRC de 2 a 8 dígitos") String nrc,
        @NotBlank(message = "Elige la actividad económica") @Size(max = 6, message = "Elige una actividad de la lista") String codActividad,
        @NotBlank(message = "Elige el tipo de establecimiento") @Pattern(regexp = "^(01|02|04|07|20)$", message = "Tipo de establecimiento no válido") String tipoEstablecimiento,
        @NotNull(message = "Elige el distrito") Integer idDistrito,
        @NotBlank(message = "Escribe la dirección") @Size(max = 200, message = "Hasta 200 caracteres") String complemento,
        @NotBlank(message = "Escribe el teléfono") @Size(max = 30, message = "Hasta 30 caracteres") String telefono,
        @NotBlank(message = "Escribe el correo") @Email(message = "Correo no válido") @Size(max = 100, message = "Hasta 100 caracteres") String correo,
        @NotBlank(message = "Escribe el código del establecimiento") @Pattern(regexp = "^[MBSP]\\d{3}$", message = "Usa una letra M, B, S o P y 3 dígitos (M001)") String codEstable,
        @NotBlank(message = "Escribe el código del punto de venta") @Pattern(regexp = "^P\\d{3}$", message = "Usa P y 3 dígitos (P001)") String codPuntoVenta
) {}
