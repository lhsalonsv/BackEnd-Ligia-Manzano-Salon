package com.ligiamanzano.salon.personal.dto;

import java.util.List;

public record EspecialidadDTO(Integer idEspecialidad, String nombreEspecialidad, List<EmpleadoResumenDTO> empleados) {}
