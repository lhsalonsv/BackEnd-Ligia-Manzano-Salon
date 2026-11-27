package com.ligiamanzano.salon.contabilidad.controller;

import com.ligiamanzano.salon.common.response.PaginaDTO;
import com.ligiamanzano.salon.contabilidad.dto.*;
import com.ligiamanzano.salon.contabilidad.service.PartidaService;
import com.ligiamanzano.salon.security.Permisos;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contabilidad/partidas")
@PreAuthorize(Permisos.LECTURA_CONTABLE)
@RequiredArgsConstructor
public class PartidaController {

    private final PartidaService service;

    @GetMapping
    public PaginaDTO<PartidaListaDTO> listar(@RequestParam(required = false) Short anio,
                                             @RequestParam(required = false) Short mes,
                                             @RequestParam(required = false) String tipo,
                                             @RequestParam(required = false) String estado,
                                             @RequestParam(required = false) String buscar,
                                             @RequestParam(defaultValue = "0") int pagina,
                                             @RequestParam(defaultValue = "50") int tamano) {
        return service.listar(anio, mes, tipo, estado, buscar, pagina, tamano);
    }

    @GetMapping("/{id}")
    public PartidaDetalleDTO obtener(@PathVariable int id) {
        return service.obtener(id);
    }

    @PostMapping
    @PreAuthorize(Permisos.CONTABILIDAD)
    public ResponseEntity<PartidaDetalleDTO> crear(@Valid @RequestBody PartidaRequest dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Permisos.CONTABILIDAD)
    public PartidaDetalleDTO actualizar(@PathVariable int id, @Valid @RequestBody PartidaRequest dto) {
        return service.actualizar(id, dto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Permisos.CONTABILIDAD)
    public ResponseEntity<Void> eliminar(@PathVariable int id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/aplicar")
    @PreAuthorize(Permisos.CONTABILIDAD)
    public NumeroPartidaDTO aplicar(@PathVariable int id) {
        return service.aplicar(id);
    }

    @PostMapping("/{id}/anular")
    @PreAuthorize(Permisos.CONTABILIDAD)
    public ResponseEntity<PartidaDetalleDTO> anular(@PathVariable int id, @Valid @RequestBody AnulacionRequest dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.anular(id, dto));
    }
}
