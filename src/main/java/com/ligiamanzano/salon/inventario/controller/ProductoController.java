package com.ligiamanzano.salon.inventario.controller;

import com.ligiamanzano.salon.inventario.dto.*;
import com.ligiamanzano.salon.inventario.service.ProductoService;
import com.ligiamanzano.salon.security.Permisos;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoService service;

    @GetMapping
    public ProductosPaginaDTO listar(@RequestParam(required = false) String buscar,
                                     @RequestParam(defaultValue = "todos") String filtro,
                                     @RequestParam(required = false) Integer idCategoria,
                                     @RequestParam(defaultValue = "0") int pagina,
                                     @RequestParam(defaultValue = "50") int tamano) {
        return service.listar(buscar, filtro, idCategoria, pagina, tamano);
    }

    @GetMapping("/{id}")
    public ProductoDetalleDTO obtener(@PathVariable int id) {
        return service.obtener(id);
    }

    @PostMapping
    @PreAuthorize(Permisos.ADMIN)
    public ResponseEntity<ProductoDetalleDTO> crear(@Valid @RequestBody ProductoRequest dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Permisos.ADMIN)
    public ProductoDetalleDTO actualizar(@PathVariable int id, @Valid @RequestBody ProductoRequest dto) {
        return service.actualizar(id, dto);
    }

    @PostMapping("/{id}/ajustes")
    @PreAuthorize(Permisos.ADMIN)
    public ProductoDetalleDTO ajustar(@PathVariable int id, @Valid @RequestBody AjusteRequest dto) {
        return service.ajustar(id, dto);
    }
}