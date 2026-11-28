package com.ligiamanzano.salon.librosiva.dto;

import java.math.BigDecimal;

public record ResumenF07DTO(
        BigDecimal debitoCcf,
        BigDecimal debitoCf,
        BigDecimal creditoFiscal,
        BigDecimal ivaPercibido,
        BigDecimal ivaRetenido,
        BigDecimal anticipoIva,
        BigDecimal remanenteAnterior,
        BigDecimal ingresosBrutos,
        BigDecimal pagoACuenta
) {}
