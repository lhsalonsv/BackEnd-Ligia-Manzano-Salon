package com.ligiamanzano.salon.contabilidad.dto;

import java.time.LocalDate;
import java.util.List;

public record EjercicioDTO(short anio, LocalDate fechaInicio, LocalDate fechaFin, String estado, List<PeriodoDTO> periodos) {}
