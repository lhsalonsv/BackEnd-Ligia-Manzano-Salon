package com.ligiamanzano.salon.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Dinero {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private Dinero() {}

    public static BigDecimal centavos(BigDecimal monto) {
        return monto.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal sinIva(BigDecimal precioConIva, BigDecimal ivaPct) {
        BigDecimal factor = BigDecimal.ONE.add(ivaPct.divide(CIEN));
        return precioConIva.divide(factor, 2, RoundingMode.HALF_UP);
    }

    public static BigDecimal comisionServicio(BigDecimal precioConIva, BigDecimal ivaPct, BigDecimal comisionPct) {
        return centavos(sinIva(precioConIva, ivaPct).multiply(comisionPct).divide(CIEN));
    }
}
