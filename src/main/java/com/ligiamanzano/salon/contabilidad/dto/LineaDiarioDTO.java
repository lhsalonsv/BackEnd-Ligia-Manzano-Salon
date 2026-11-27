package com.ligiamanzano.salon.contabilidad.dto;

import java.math.BigDecimal;

public record LineaDiarioDTO(short linea, String codigoCuenta, String nombreCuenta, String concepto, BigDecimal debe, BigDecimal haber) {}
