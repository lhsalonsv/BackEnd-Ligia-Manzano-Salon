package com.ligiamanzano.salon.inventario.dto;

import java.math.BigDecimal;

public record AjusteFisicoDTO(int idProducto, long cantidad, BigDecimal valor) {}
