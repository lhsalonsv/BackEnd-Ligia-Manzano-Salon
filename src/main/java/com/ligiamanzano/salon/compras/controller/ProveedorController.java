package com.ligiamanzano.salon.compras.controller;

import com.ligiamanzano.salon.common.request.ActivoRequest;
import com.ligiamanzano.salon.compras.dto.ProveedorFichaDTO;
import com.ligiamanzano.salon.compras.dto.ProveedorListaDTO;
import com.ligiamanzano.salon.compras.dto.ProveedorRequest;
import com.ligiamanzano.salon.compras.dto.UltimaCuentaDTO;
import com.ligiamanzano.salon.compras.service.ProveedorService;
import com.ligiamanzano.salon.security.Permisos;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/proveedores")
@RequiredArgsConstructor
public class ProveedorController {

    private final ProveedorService service;

    @GetMapping
    public List<ProveedorListaDTO> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public ProveedorFichaDTO obtener(@PathVariable int id) {
        return service.obtener(id);
    }

    @GetMapping("/{id}/ultima-cuenta")
    @PreAuthorize(Permisos.CONTABILIDAD)
    public UltimaCuentaDTO ultimaCuenta(@PathVariable int id) {
        return service.ultimaCuenta(id);
    }

    @PostMapping
    @PreAuthorize(Permisos.ADMIN)
    public ResponseEntity<ProveedorFichaDTO> crear(@Valid @RequestBody ProveedorRequest dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Permisos.ADMIN)
    public ProveedorFichaDTO actualizar(@PathVariable int id, @Valid @RequestBody ProveedorRequest dto) {
        return service.actualizar(id, dto);
    }

    @PatchMapping("/{id}/activo")
    @PreAuthorize(Permisos.ADMIN)
    public ProveedorFichaDTO cambiarActivo(@PathVariable int id, @Valid @RequestBody ActivoRequest dto) {
        return service.cambiarActivo(id, dto.activo());
    }
}