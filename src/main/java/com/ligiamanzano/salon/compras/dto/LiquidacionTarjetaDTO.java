package com.ligiamanzano.salon.compras.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LiquidacionTarjetaDTO(
        Integer idLiquidacion,
        LocalDate fecha,
        Integer idProveedor,
        String nombreProveedor,
        String numeroDocumento,
        String codigoGeneracion,
        LocalDate periodoIva,
        BigDecimal montoLiquidado,
        BigDecimal anticipoIva,
        BigDecimal comisionTotal,
        BigDecimal montoDepositado,
        String codigoCuentaBanco,
        String nombreCuentaBanco,
        Integer idCompraComision,
        boolean contabilizada
) {}
