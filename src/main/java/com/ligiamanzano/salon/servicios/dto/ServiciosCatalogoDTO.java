package com.ligiamanzano.salon.servicios.dto;

import java.math.BigDecimal;
import java.util.List;

public record ServiciosCatalogoDTO(BigDecimal comisionServicioPct, BigDecimal ivaPct, List<ServicioDTO> servicios) {}
