package com.ligiamanzano.salon.personal.dto;

import com.ligiamanzano.salon.configuracion.dto.HorarioSalonDTO;

import java.util.List;

public record EmpleadosRespuestaDTO(
        QuincenaDTO quincena,
        List<HorarioSalonDTO> horarioSalon,
        List<EmpleadoListaDTO> empleados
) {}
