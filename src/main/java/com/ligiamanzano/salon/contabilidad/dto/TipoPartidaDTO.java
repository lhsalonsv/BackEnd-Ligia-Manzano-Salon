package com.ligiamanzano.salon.contabilidad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TipoPartidaDTO(
        @NotBlank(message = "Escribe el código") @Pattern(regexp = "^[A-Z]{3}$", message = "Son 3 letras mayúsculas, por ejemplo PDA") String codigo,
        @NotBlank(message = "Escribe el nombre del tipo") @Size(max = 60, message = "El nombre llega a 60 caracteres") String nombre,
        boolean predeterminado,
        boolean activo
) {}
