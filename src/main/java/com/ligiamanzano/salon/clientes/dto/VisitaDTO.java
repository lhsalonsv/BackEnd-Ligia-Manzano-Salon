package com.ligiamanzano.salon.clientes.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record VisitaDTO(LocalDate fecha, String descripcion, String empleado, BigDecimal total) {}
