package com.ligiamanzano.salon.personal.controller;

import com.ligiamanzano.salon.personal.dto.BajaRequest;
import com.ligiamanzano.salon.personal.dto.EmpleadoFichaDTO;
import com.ligiamanzano.salon.personal.dto.EmpleadoRequest;
import com.ligiamanzano.salon.personal.dto.EmpleadosRespuestaDTO;
import com.ligiamanzano.salon.personal.service.EmpleadoService;
import com.ligiamanzano.salon.security.Permisos;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/empleados")
@PreAuthorize(Permisos.ADMIN) //Solo administradores estan autorizados
@RequiredArgsConstructor
public class EmpleadoController {

    private final EmpleadoService service;

    @GetMapping
    public EmpleadosRespuestaDTO listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public EmpleadoFichaDTO obtener(@PathVariable int id) {
        return service.obtener(id);
    }

    @PostMapping
    public ResponseEntity<EmpleadoFichaDTO> crear(@Valid @RequestBody EmpleadoRequest dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(dto));
    }

    @PutMapping("/{id}")
    public EmpleadoFichaDTO actualizar(@PathVariable int id, @Valid @RequestBody EmpleadoRequest dto) {
        return service.actualizar(id, dto);
    }

    @PatchMapping("/{id}/baja")
    public EmpleadoFichaDTO darDeBaja(@PathVariable int id, @Valid @RequestBody BajaRequest dto) {
        return service.darDeBaja(id, dto.fechaBaja());
    }
}