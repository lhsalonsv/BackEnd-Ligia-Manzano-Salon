package com.ligiamanzano.salon.contabilidad.dto;

import com.ligiamanzano.salon.configuracion.dto.EmisorEncabezadoDTO;

import java.util.List;

public record SaldosDTO(EmisorEncabezadoDTO emisor, List<SaldoCuentaDTO> filas) {}
