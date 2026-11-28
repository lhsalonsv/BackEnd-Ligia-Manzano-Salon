package com.ligiamanzano.salon.compras.controller;

import com.ligiamanzano.salon.compras.dto.LiquidacionTarjetaDTO;
import com.ligiamanzano.salon.compras.dto.LiquidacionTarjetaRequest;
import com.ligiamanzano.salon.compras.dto.LiquidacionesTarjetaDTO;
import com.ligiamanzano.salon.compras.service.LiquidacionTarjetaService;
import com.ligiamanzano.salon.security.Permisos;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/liquidaciones-tarjeta")
@PreAuthorize(Permisos.LECTURA_CONTABLE)
@RequiredArgsConstructor
public class LiquidacionTarjetaController {

    private final LiquidacionTarjetaService service;

    @GetMapping
    public LiquidacionesTarjetaDTO listar(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return service.listar(desde, hasta);
    }

    @PostMapping
    @PreAuthorize(Permisos.CONTABILIDAD)
    public ResponseEntity<LiquidacionTarjetaDTO> crear(@Valid @RequestBody LiquidacionTarjetaRequest dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Permisos.CONTABILIDAD)
    public LiquidacionTarjetaDTO actualizar(@PathVariable int id, @Valid @RequestBody LiquidacionTarjetaRequest dto) {
        return service.actualizar(id, dto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Permisos.CONTABILIDAD)
    public ResponseEntity<Void> borrar(@PathVariable int id) {
        service.borrar(id);
        return ResponseEntity.noContent().build();
    }
}
