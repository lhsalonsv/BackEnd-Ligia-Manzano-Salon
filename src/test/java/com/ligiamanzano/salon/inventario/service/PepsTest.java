package com.ligiamanzano.salon.inventario.service;

import com.ligiamanzano.salon.common.exception.ConflictoException;
import com.ligiamanzano.salon.inventario.service.Peps.LoteDisponible;
import com.ligiamanzano.salon.inventario.service.Peps.Salida;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PepsTest {

    private static final List<LoteDisponible> LOTES = List.of(
            new LoteDisponible(1, 3, new BigDecimal("6.5000")),
            new LoteDisponible(2, 12, new BigDecimal("6.8000")),
            new LoteDisponible(3, 5, new BigDecimal("7.0000")));

    @Test
    void saleDelLoteMasAntiguoPrimero() {
        List<Salida> salidas = Peps.repartir(LOTES, 2);
        assertEquals(List.of(new Salida(1, 2, new BigDecimal("6.5000"))), salidas);
    }

    @Test
    void pasaAlSiguienteLoteCuandoUnoNoAlcanza() {
        List<Salida> salidas = Peps.repartir(LOTES, 16);
        assertEquals(List.of(
                new Salida(1, 3, new BigDecimal("6.5000")),
                new Salida(2, 12, new BigDecimal("6.8000")),
                new Salida(3, 1, new BigDecimal("7.0000"))), salidas);
        BigDecimal costo = salidas.stream().map(Salida::costo).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, new BigDecimal("108.1").compareTo(costo));
    }

    @Test
    void stockExactoVaciaTodosLosLotes() {
        List<Salida> salidas = Peps.repartir(LOTES, 20);
        assertEquals(20, salidas.stream().mapToInt(Salida::cantidad).sum());
        assertEquals(3, salidas.size());
    }

    @Test
    void stockInsuficienteEs409() {
        ConflictoException e = assertThrows(ConflictoException.class, () -> Peps.repartir(LOTES, 21));
        assertEquals("STOCK_INSUFICIENTE", e.getCodigo());
        assertEquals("Solo hay 20 en existencia.", e.getMessage());
        ConflictoException vacio = assertThrows(ConflictoException.class, () -> Peps.repartir(List.of(), 1));
        assertEquals("No hay existencia.", vacio.getMessage());
    }

    @Test
    void cantidadCeroNoSePermite() {
        assertThrows(IllegalArgumentException.class, () -> Peps.repartir(LOTES, 0));
    }
}
