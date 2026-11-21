package com.ligiamanzano.salon.compras.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UltimaCompraDTO(LocalDate fechaCompra, BigDecimal total) {}
