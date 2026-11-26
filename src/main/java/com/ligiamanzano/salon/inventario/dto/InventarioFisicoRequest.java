package com.ligiamanzano.salon.inventario.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record InventarioFisicoRequest(@NotNull(message = "Escribe la fecha de corte") LocalDate fechaCorte) {}
