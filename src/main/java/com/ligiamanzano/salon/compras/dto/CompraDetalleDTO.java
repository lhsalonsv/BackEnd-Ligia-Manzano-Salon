package com.ligiamanzano.salon.compras.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record CompraDetalleDTO(
        Integer idCompra,
        String numeroCompra,
        Integer idProveedor,
        String nombreProveedor,
        LocalDate fechaCompra,
        String tipoDocumentoProveedor,
        String numeroDocumentoProveedor,
        BigDecimal subtotalSinIva,
        BigDecimal iva,
        BigDecimal total,
        LocalDate fechaVencimientoPago,
        String estado,
        String nombreUsuario,
        OffsetDateTime creadoEn,
        List<LineaCompraDTO> detalle
) {}
