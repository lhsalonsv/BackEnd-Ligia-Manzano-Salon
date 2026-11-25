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
        Short claseDocumento,
        String codigoGeneracionProveedor,
        Integer idCompraRelacionada,
        String numeroCompraRelacionada,
        LocalDate periodoIva,
        BigDecimal subtotalSinIva,
        BigDecimal comprasExentas,
        BigDecimal iva,
        BigDecimal ivaPercibido,
        BigDecimal total,
        Short tipoOperacionRenta,
        Short clasificacionRenta,
        Short sectorRenta,
        Short tipoCostoGasto,
        LocalDate fechaVencimientoPago,
        String estado,
        String nombreUsuario,
        OffsetDateTime creadoEn,
        List<LineaCompraDTO> detalle
) {}
