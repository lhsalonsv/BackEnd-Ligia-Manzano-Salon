package com.ligiamanzano.salon.bitacora.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EquipoTest {

    @Test
    void reconoceLosNavegadoresComunes() {
        assertEquals("Chrome en Windows", Equipo.desde(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"));
        assertEquals("Edge en Windows", Equipo.desde(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0"));
        assertEquals("Safari en iPhone", Equipo.desde(
                "Mozilla/5.0 (iPhone; CPU iPhone OS 18_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.1 Mobile/15E148 Safari/604.1"));
        assertEquals("Chrome en Android", Equipo.desde(
                "Mozilla/5.0 (Linux; Android 14; SM-A546E) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"));
        assertEquals("Firefox en Mac", Equipo.desde(
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 14.6; rv:132.0) Gecko/20100101 Firefox/132.0"));
        assertEquals("PowerShell en Windows", Equipo.desde(
                "Mozilla/5.0 (Windows NT 10.0; Microsoft Windows 10.0.26200; es-SV) WindowsPowerShell/5.1.26100"));
    }

    @Test
    void sinDatosEsDesconocido() {
        assertEquals(Equipo.DESCONOCIDO, Equipo.desde(null));
        assertEquals(Equipo.DESCONOCIDO, Equipo.desde("   "));
        assertEquals(Equipo.DESCONOCIDO, Equipo.desde("algo-raro/1.0"));
    }
}
