package com.ligiamanzano.salon.compras.controller;

import com.ligiamanzano.salon.compras.dto.CompraDetalleDTO;
import com.ligiamanzano.salon.compras.dto.CompraRequest;
import com.ligiamanzano.salon.compras.dto.ComprasPaginaDTO;
import com.ligiamanzano.salon.compras.service.CompraService;
import com.ligiamanzano.salon.security.Permisos;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/compras")
@RequiredArgsConstructor
public class CompraController {

    private final CompraService service;

    @GetMapping
    public ComprasPaginaDTO listar(@RequestParam(required = false) String buscar,
                                   @RequestParam(defaultValue = "todas") String filtro,
                                   @RequestParam(required = false) Integer idProveedor,
                                   @RequestParam(defaultValue = "0") int pagina,
                                   @RequestParam(defaultValue = "50") int tamano) {
        return service.listar(buscar, filtro, idProveedor, pagina, tamano);
    }

    @GetMapping("/{id}")
    public CompraDetalleDTO obtener(@PathVariable int id) {
        return service.obtener(id);
    }

    @PostMapping
    @PreAuthorize(Permisos.ADMIN)
    public ResponseEntity<CompraDetalleDTO> registrar(@Valid @RequestBody CompraRequest dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrar(dto));
    }
}
