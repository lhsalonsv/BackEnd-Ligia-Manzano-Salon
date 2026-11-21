package com.ligiamanzano.salon.inventario.dto;

import jakarta.validation.constraints.*;

public record AjusteRequest(
        @NotBlank @Pattern(regexp = "^(DANADO|USO_SALON|CONTEO)$", message = "Tipo de ajuste no válido") String tipo,
        @NotNull(message = "Escribe la cantidad") @Min(value = 0, message = "No puede ser negativa") Integer cantidad,
        @NotBlank(message = "Escribe el motivo") @Size(max = 150) String motivo
) {}
