package com.ligiamanzano.salon.contabilidad.dto;

import java.math.BigDecimal;

public record SaldoCuentaDTO(
        String codigo,
        String nombre,
        String tipo,
        String naturaleza,
        short nivel,
        boolean aceptaMovimiento,
        BigDecimal saldoInicial,
        BigDecimal debe,
        BigDecimal haber,
        BigDecimal saldoFinal
) {}
