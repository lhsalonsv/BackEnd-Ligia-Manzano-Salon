package com.ligiamanzano.salon.compras.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CompraListaDTO(
        Integer idCompra,
        String numeroCompra,
        LocalDate fechaCompra,
        Integer idProveedor,
        String nombreProveedor,
        String tipoDocumentoProveedor,
        String numeroDocumentoProveedor,
        int productos,
        int unidades,
        BigDecimal total,
        String estado
) {}
