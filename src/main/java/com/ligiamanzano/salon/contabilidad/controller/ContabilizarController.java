package com.ligiamanzano.salon.contabilidad.controller;

import com.ligiamanzano.salon.contabilidad.dto.ContabilizadoDTO;
import com.ligiamanzano.salon.contabilidad.dto.ContabilizarRequest;
import com.ligiamanzano.salon.contabilidad.dto.VistaPreviaDTO;
import com.ligiamanzano.salon.contabilidad.service.ContabilizarService;
import com.ligiamanzano.salon.security.Permisos;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/contabilidad/contabilizar")
@PreAuthorize(Permisos.LECTURA_CONTABLE)
@RequiredArgsConstructor
public class ContabilizarController {

    private final ContabilizarService service;

    @GetMapping("/{origen}")
    public VistaPreviaDTO vistaPrevia(@PathVariable String origen,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return service.vistaPrevia(origen, desde, hasta);
    }

    @PostMapping("/{origen}")
    @PreAuthorize(Permisos.CONTABILIDAD)
    public ContabilizadoDTO contabilizar(@PathVariable String origen, @Valid @RequestBody ContabilizarRequest dto) {
        return service.contabilizar(origen, dto);
    }
}
