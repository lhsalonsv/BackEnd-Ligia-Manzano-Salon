package com.ligiamanzano.salon.contabilidad.dto;

import com.ligiamanzano.salon.configuracion.dto.EmisorEncabezadoDTO;

import java.math.BigDecimal;
import java.util.List;

public record DiarioDTO(EmisorEncabezadoDTO emisor, List<PartidaDiarioDTO> partidas, BigDecimal totalDebe, BigDecimal totalHaber) {}
