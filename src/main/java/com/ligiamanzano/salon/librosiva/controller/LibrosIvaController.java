package com.ligiamanzano.salon.librosiva.controller;

import com.ligiamanzano.salon.librosiva.dto.LibrosIvaDTO;
import com.ligiamanzano.salon.librosiva.dto.ResumenF07DTO;
import com.ligiamanzano.salon.librosiva.service.LibrosIvaService;
import com.ligiamanzano.salon.security.Permisos;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/libros-iva")
@PreAuthorize(Permisos.LECTURA_CONTABLE)
@RequiredArgsConstructor
public class LibrosIvaController {

    private final LibrosIvaService service;

    @GetMapping
    public LibrosIvaDTO obtener(@RequestParam String periodo) {
        return service.obtener(periodo);
    }

    @GetMapping("/{periodo}/resumen-f07")
    public ResumenF07DTO resumenF07(@PathVariable String periodo) {
        return service.resumenF07(periodo);
    }
}
