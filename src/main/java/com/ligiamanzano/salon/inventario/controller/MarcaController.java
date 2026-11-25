package com.ligiamanzano.salon.inventario.controller;

import com.ligiamanzano.salon.inventario.dto.MarcaDTO;
import com.ligiamanzano.salon.inventario.dto.MarcaRequest;
import com.ligiamanzano.salon.inventario.service.MarcaService;
import com.ligiamanzano.salon.security.Permisos;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/marcas")
@RequiredArgsConstructor
public class MarcaController {

    private final MarcaService service;

    @GetMapping
    public List<MarcaDTO> listar() {
        return service.listar();
    }

    @PostMapping
    @PreAuthorize(Permisos.ADMIN)
    public ResponseEntity<MarcaDTO> crear(@Valid @RequestBody MarcaRequest dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(dto));
    }
}