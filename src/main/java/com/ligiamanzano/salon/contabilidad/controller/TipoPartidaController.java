package com.ligiamanzano.salon.contabilidad.controller;

import com.ligiamanzano.salon.contabilidad.dto.TipoPartidaDTO;
import com.ligiamanzano.salon.contabilidad.service.TipoPartidaService;
import com.ligiamanzano.salon.security.Permisos;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contabilidad/tipos-partida")
@PreAuthorize(Permisos.LECTURA_CONTABLE)
@RequiredArgsConstructor
public class TipoPartidaController {

    private final TipoPartidaService service;

    @GetMapping
    public List<TipoPartidaDTO> listar() {
        return service.listar();
    }

    @PostMapping
    @PreAuthorize(Permisos.CONTABILIDAD)
    public ResponseEntity<TipoPartidaDTO> crear(@Valid @RequestBody TipoPartidaDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(dto));
    }

    @PutMapping("/{codigo}")
    @PreAuthorize(Permisos.CONTABILIDAD)
    public TipoPartidaDTO actualizar(@PathVariable String codigo, @Valid @RequestBody TipoPartidaDTO dto) {
        return service.actualizar(codigo, dto);
    }
}
