package com.ligiamanzano.salon.inventario.dto;

import jakarta.validation.constraints.Size;

public record CierreRequest(@Size(max = 500, message = "Hasta 500 caracteres") String observaciones) {}
