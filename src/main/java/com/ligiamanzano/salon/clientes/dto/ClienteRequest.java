package com.ligiamanzano.salon.clientes.dto;

import jakarta.validation.constraints.*;

public record ClienteRequest(
        @NotBlank(message = "Escribe el nombre") @Size(max = 80) String nombre,
        @Size(max = 80) String apellido,
        @NotBlank(message = "Escribe el teléfono")
        @Pattern(regexp = "^(\\d{4}-\\d{4}|\\+\\d{8,15})$", message = "Usa 7000-0000 o +código y número") String telefono,
        @Email(message = "Correo no válido") @Size(max = 100) String correo,
        @Min(1) @Max(31) Short diaCumple,
        @Min(1) @Max(12) Short mesCumple,
        @Size(max = 300) String alergias,
        String tipoDocumento,
        @Pattern(regexp = "^\\d{8}-\\d$", message = "DUI con formato 00000000-0") String numDocumento,
        @Size(max = 250) String razonSocial,
        @Size(max = 150) String nombreComercial,
        @Pattern(regexp = "^(\\d{14}|\\d{9})$", message = "NIT de 14 dígitos (o 9 si es DUI homologado)") String nit,
        @Pattern(regexp = "^\\d{2,8}$", message = "NRC de 2 a 8 dígitos") String nrc,
        String codActividad,
        Integer idDistrito,
        @Size(max = 200) String complemento
) {}
