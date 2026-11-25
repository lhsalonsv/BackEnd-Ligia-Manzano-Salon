package com.ligiamanzano.salon.inventario.service;

import com.ligiamanzano.salon.common.exception.ConflictoException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class Peps {

    public record LoteDisponible(int idLote, int disponible, BigDecimal costoUnitario) {}

    public record Salida(int idLote, int cantidad, BigDecimal costoUnitario) {

        public BigDecimal costo() {
            return costoUnitario.multiply(BigDecimal.valueOf(cantidad));
        }
    }

    private Peps() {}

    public static List<Salida> repartir(List<LoteDisponible> lotes, int cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad tiene que ser mayor que 0");
        }
        int existencia = lotes.stream().mapToInt(LoteDisponible::disponible).sum();
        if (existencia < cantidad) {
            String mensaje = existencia == 0 ? "No hay existencia." : "Solo hay " + existencia + " en existencia.";
            throw new ConflictoException(mensaje, "STOCK_INSUFICIENTE", Map.of("cantidad", mensaje));
        }
        //Lista para anotar de qué lotes se sacará la mercancía y lleva un contador de cuánto falta por completar
        List<Salida> salidas = new ArrayList<>();
        int falta = cantidad;
        for (LoteDisponible lote : lotes) {
            if (falta == 0) break;
            int toma = Math.min(falta, lote.disponible());
            if (toma > 0) {
                salidas.add(new Salida(lote.idLote(), toma, lote.costoUnitario()));
                falta -= toma;
            }
        }
        return salidas;
    }
}