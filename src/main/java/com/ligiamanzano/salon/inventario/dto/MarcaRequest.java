package com.ligiamanzano.salon.inventario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MarcaRequest(@NotBlank(message = "Escribe la marca") @Size(max = 60) String nombreMarca) {}
