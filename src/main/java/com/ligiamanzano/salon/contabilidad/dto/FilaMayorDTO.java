package com.ligiamanzano.salon.contabilidad.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FilaMayorDTO(
        int fila,
        LocalDate fecha,
        Integer idPartida,
        String numero,
        String codigoTipo,
        String codigoCuenta,
        String concepto,
        BigDecimal debe,
        BigDecimal haber,
        BigDecimal saldo
) {}
