package com.ligiamanzano.salon.inventario.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MovimientoRegistroDTO(
        long idMovimiento,
        long correlativo,
        LocalDate fecha,
        String tipo,
        String descripcionOperacion,
        String tipoDocumento,
        String numeroDocumento,
        String proveedor,
        String nacionalidadProveedor,
        String referenciaCosto,
        int idLote,
        BigDecimal costoUnitario,
        int entradaUnidades,
        int salidaUnidades,
        BigDecimal entradaValor,
        BigDecimal salidaValor,
        long saldoUnidades,
        BigDecimal saldoValor
) {}
