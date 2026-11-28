package com.ligiamanzano.salon.contabilidad.controller;

import com.ligiamanzano.salon.contabilidad.dto.ImpuestosDTO;
import com.ligiamanzano.salon.contabilidad.service.ContabilizarService;
import com.ligiamanzano.salon.security.Permisos;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contabilidad/impuestos")
@PreAuthorize(Permisos.CONTABILIDAD)
@RequiredArgsConstructor
public class ImpuestosController {

    private final ContabilizarService service;

    @PostMapping("/{anio}/{mes}")
    public ResponseEntity<ImpuestosDTO> generar(@PathVariable short anio, @PathVariable short mes) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.generarImpuestos(anio, mes));
    }
}
