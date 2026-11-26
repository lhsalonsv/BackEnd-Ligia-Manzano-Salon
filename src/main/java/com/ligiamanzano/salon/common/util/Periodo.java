package com.ligiamanzano.salon.common.util;

import com.ligiamanzano.salon.common.exception.OperacionInvalidaException;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Map;

public record Periodo(YearMonth mes) {

    public static Periodo desde(String texto) {
        YearMonth mes;
        try {
            mes = YearMonth.parse(texto == null ? "" : texto.trim());
        } catch (DateTimeParseException e) {
            throw error("Escribe el mes como AAAA-MM (por ejemplo 2026-11).");
        }
        if (mes.isAfter(YearMonth.from(Fechas.hoy()))) {
            throw error("No puede ser un mes futuro.");
        }
        return new Periodo(mes);
    }

    public LocalDate primerDia() {
        return mes.atDay(1);
    }

    public LocalDate primerDiaDelSiguiente() {
        return mes.plusMonths(1).atDay(1);
    }

    public String texto() {
        return mes.toString();
    }

    private static OperacionInvalidaException error(String mensaje) {
        return new OperacionInvalidaException(mensaje, "DATOS_INVALIDOS", Map.of("periodo", mensaje));
    }
}
