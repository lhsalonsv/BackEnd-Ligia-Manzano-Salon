package com.ligiamanzano.salon.compras.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CompraResumenDTO(
        Integer idCompra,
        String numeroCompra,
        LocalDate fechaCompra,
        int productos,
        BigDecimal total,
        LocalDate fechaVencimientoPago
) {}
