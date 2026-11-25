package com.ligiamanzano.salon.compras.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CompraResumenDTO(
        Integer idCompra,
        String numeroCompra,
        LocalDate fechaCompra,
        String tipoDocumentoProveedor,
        int productos,
        int gastos,
        BigDecimal total,
        LocalDate fechaVencimientoPago
) {}
