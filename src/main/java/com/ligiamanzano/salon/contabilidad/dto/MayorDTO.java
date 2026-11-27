package com.ligiamanzano.salon.contabilidad.dto;

import com.ligiamanzano.salon.configuracion.dto.EmisorEncabezadoDTO;

import java.util.List;

public record MayorDTO(EmisorEncabezadoDTO emisor, CuentaMayorDTO cuenta, List<FilaMayorDTO> filas) {}
