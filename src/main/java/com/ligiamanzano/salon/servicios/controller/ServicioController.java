package com.ligiamanzano.salon.servicios.controller;

import com.ligiamanzano.salon.common.request.ActivoRequest;
import com.ligiamanzano.salon.security.Permisos;
import com.ligiamanzano.salon.servicios.dto.ServicioDetalleDTO;
import com.ligiamanzano.salon.servicios.dto.ServicioRequest;
import com.ligiamanzano.salon.servicios.dto.ServiciosCatalogoDTO;
import com.ligiamanzano.salon.servicios.service.ServicioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/servicios")
@RequiredArgsConstructor
public class ServicioController {

    private final ServicioService service;

    @GetMapping
    public ServiciosCatalogoDTO listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public ServicioDetalleDTO obtener(@PathVariable int id) {
        return service.obtener(id);
    }

    @PostMapping
    @PreAuthorize(Permisos.ADMIN)
    public ResponseEntity<ServicioDetalleDTO> crear(@Valid @RequestBody ServicioRequest dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Permisos.ADMIN)
    public ServicioDetalleDTO actualizar(@PathVariable int id, @Valid @RequestBody ServicioRequest dto) {
        return service.actualizar(id, dto);
    }

    @PatchMapping("/{id}/activo")
    @PreAuthorize(Permisos.ADMIN)
    public ServicioDetalleDTO cambiarActivo(@PathVariable int id, @Valid @RequestBody ActivoRequest dto) {
        return service.cambiarActivo(id, dto.activo());
    }
}
