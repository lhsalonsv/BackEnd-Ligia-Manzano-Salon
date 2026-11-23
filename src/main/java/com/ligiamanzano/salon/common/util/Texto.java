package com.ligiamanzano.salon.common.util;

import java.util.Locale;

public final class Texto {

    private Texto() {}

    public static String limpio(String valor) {
        if (valor == null) return null;
        String recortado = valor.trim();
        return recortado.isEmpty() ? null : recortado;
    }

    public static String minusculas(String valor) {
        String recortado = limpio(valor);
        return recortado == null ? null : recortado.toLowerCase(Locale.ROOT);
    }

    public static String soloDigitos(String valor) {
        return valor == null ? "" : valor.replaceAll("\\D", "");
    }
}
