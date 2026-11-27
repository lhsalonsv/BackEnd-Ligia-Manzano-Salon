package com.ligiamanzano.salon.contabilidad.dto;

import java.math.BigDecimal;

public record LineaPartidaRequest(String codigoCuenta, String concepto, BigDecimal debe, BigDecimal haber) {}
