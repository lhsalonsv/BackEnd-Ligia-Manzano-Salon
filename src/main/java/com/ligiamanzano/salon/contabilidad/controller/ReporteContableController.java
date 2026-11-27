package com.ligiamanzano.salon.contabilidad.controller;

import com.ligiamanzano.salon.contabilidad.dto.DiarioDTO;
import com.ligiamanzano.salon.contabilidad.dto.MayorDTO;
import com.ligiamanzano.salon.contabilidad.dto.SaldosDTO;
import com.ligiamanzano.salon.contabilidad.service.ReporteContableService;
import com.ligiamanzano.salon.security.Permisos;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/contabilidad/reportes")
@PreAuthorize(Permisos.LECTURA_CONTABLE)
@RequiredArgsConstructor
public class ReporteContableController {

    private final ReporteContableService service;

    @GetMapping("/saldos")
    public SaldosDTO saldos(@RequestParam short anio, @RequestParam short mes, @RequestParam(required = false) Short nivel) {
        return service.saldos(anio, mes, nivel);
    }

    @GetMapping("/mayor")
    public MayorDTO mayor(@RequestParam String cuenta,
                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return service.mayor(cuenta, desde, hasta);
    }

    @GetMapping("/diario")
    public DiarioDTO diario(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                            @RequestParam(required = false) String tipo) {
        return service.diario(desde, hasta, tipo);
    }
}
