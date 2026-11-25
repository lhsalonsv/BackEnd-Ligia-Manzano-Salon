package com.ligiamanzano.salon.compras.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CompraRequest(
        @NotNull(message = "Elige el proveedor") Integer idProveedor,
        @NotNull(message = "Escribe la fecha") @PastOrPresent(message = "No puede ser una fecha futura") LocalDate fechaCompra,
        @NotBlank(message = "Elige el tipo de documento") @Pattern(regexp = "^(CCF|FACTURA|NC|ND|OTRO)$", message = "Elige CCF, FACTURA, NC, ND u OTRO") String tipoDocumentoProveedor,
        @Size(max = 40, message = "Hasta 40 caracteres") String numeroDocumentoProveedor,
        @NotNull(message = "Elige si es DTE o impreso") Short claseDocumento,
        @Pattern(regexp = "^[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}$", message = "El código de generación tiene 36 caracteres") String codigoGeneracionProveedor,
        Integer idCompraRelacionada,
        @NotNull(message = "Elige el mes en que se declara") LocalDate periodoIva,
        @DecimalMin(value = "0.00", message = "No puede ser negativo") @Digits(integer = 10, fraction = 2, message = "Hasta 2 decimales") BigDecimal comprasExentas,
        Boolean percepcion,
        @NotNull(message = "Elige el tipo de operación") Short tipoOperacionRenta,
        @NotNull(message = "Elige la clasificación") Short clasificacionRenta,
        @NotNull(message = "Elige el sector") Short sectorRenta,
        @NotNull(message = "Elige el tipo de costo o gasto") Short tipoCostoGasto,
        @NotEmpty(message = "Agrega al menos una línea") List<@Valid LineaCompraRequest> detalle
) {}
