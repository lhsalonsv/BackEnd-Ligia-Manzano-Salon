package com.ligiamanzano.salon.inventario.controller;

import com.ligiamanzano.salon.inventario.dto.*;
import com.ligiamanzano.salon.inventario.service.InventarioFisicoService;
import com.ligiamanzano.salon.security.Permisos;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventario-fisico")
@PreAuthorize(Permisos.LECTURA_CONTABLE)
@RequiredArgsConstructor
public class InventarioFisicoController {

    private final InventarioFisicoService service;

    @GetMapping
    public List<InventarioFisicoListaDTO> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public InventarioFisicoDTO obtener(@PathVariable int id) {
        return service.obtener(id);
    }

    @PostMapping
    @PreAuthorize(Permisos.CONTABILIDAD)
    public ResponseEntity<InventarioFisicoDTO> crear(@Valid @RequestBody InventarioFisicoRequest dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(dto));
    }

    @PutMapping("/{id}/conteo")
    @PreAuthorize(Permisos.CONTABILIDAD)
    public InventarioFisicoDTO guardarConteo(@PathVariable int id, @RequestBody List<ConteoRequest> dto) {
        return service.guardarConteo(id, dto);
    }

    @PostMapping("/{id}/cerrar")
    @PreAuthorize(Permisos.CONTABILIDAD)
    public InventarioFisicoDTO cerrar(@PathVariable int id, @Valid @RequestBody(required = false) CierreRequest dto) {
        return service.cerrar(id, dto);
    }
}
