package com.ligiamanzano.salon.clientes.controller;

import com.ligiamanzano.salon.clientes.dto.ClienteFichaDTO;
import com.ligiamanzano.salon.clientes.dto.ClienteRequest;
import com.ligiamanzano.salon.clientes.dto.ClientesPaginaDTO;
import com.ligiamanzano.salon.clientes.service.ClienteService;
import com.ligiamanzano.salon.security.Permisos;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService service;

    @GetMapping
    public ClientesPaginaDTO listar(@RequestParam(required = false) String buscar, @RequestParam(defaultValue = "todos") String filtro, @RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "50") int tamano) {
        return service.listar(buscar, filtro, pagina, tamano);
    }

    @GetMapping("/{id}")
    public ClienteFichaDTO obtener(@PathVariable int id) {
        return service.obtener(id);
    }

    @PostMapping
    @PreAuthorize(Permisos.CAJA)
    public ResponseEntity<ClienteFichaDTO> crear(@Valid @RequestBody ClienteRequest dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Permisos.CAJA)
    public ClienteFichaDTO actualizar(@PathVariable int id, @Valid @RequestBody ClienteRequest dto) {
        return service.actualizar(id, dto);
    }
}