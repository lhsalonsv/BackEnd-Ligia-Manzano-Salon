package com.ligiamanzano.salon.clientes.dto;

import java.time.LocalDate;

public record FormulaResumenDTO(LocalDate fecha, String empleado, String formula, String observaciones) {}
