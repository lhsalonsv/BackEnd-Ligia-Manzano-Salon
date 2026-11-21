package com.ligiamanzano.salon.configuracion.dto;

import jakarta.validation.constraints.*;

public record NegocioRequest(
        @Size(max = 150) String nombreComercial,
        @NotBlank(message = "Escribe el nombre o razón social") @Size(max = 250) String nombre,
        @NotBlank(message = "Escribe el NIT") @Pattern(regexp = "^(\\d{14}|\\d{9})$", message = "NIT de 14 dígitos (o 9 si es DUI homologado)") String nit,
        @NotBlank(message = "Escribe el NRC") @Pattern(regexp = "^\\d{2,8}$", message = "NRC de 2 a 8 dígitos") String nrc,
        @NotBlank(message = "Elige la actividad económica") @Size(max = 6) String codActividad,
        @NotBlank @Pattern(regexp = "^(01|02|04|07|20)$", message = "Tipo de establecimiento no válido") String tipoEstablecimiento,
        @NotNull(message = "Elige el distrito") Integer idDistrito,
        @NotBlank(message = "Escribe la dirección") @Size(max = 200) String complemento,
        @NotBlank(message = "Escribe el teléfono") @Size(max = 30) String telefono,
        @NotBlank(message = "Escribe el correo") @Email(message = "Correo no válido") @Size(max = 100) String correo,
        @NotBlank @Pattern(regexp = "^[MBSP]\\d{3}$", message = "Usa una letra M, B, S o P y 3 dígitos (M001)") String codEstable,
        @NotBlank @Pattern(regexp = "^P\\d{3}$", message = "Usa P y 3 dígitos (P001)") String codPuntoVenta
) {}
