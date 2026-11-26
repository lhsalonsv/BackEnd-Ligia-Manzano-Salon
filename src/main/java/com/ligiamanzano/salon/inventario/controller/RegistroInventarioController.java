package com.ligiamanzano.salon.inventario.controller;

import com.ligiamanzano.salon.inventario.dto.RegistroInventarioDTO;
import com.ligiamanzano.salon.inventario.service.RegistroInventarioService;
import com.ligiamanzano.salon.security.Permisos;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventario/registro")
@PreAuthorize(Permisos.LECTURA_CONTABLE)
@RequiredArgsConstructor
public class RegistroInventarioController {

    private final RegistroInventarioService service;

    @GetMapping
    public RegistroInventarioDTO obtener(@RequestParam String periodo, @RequestParam(required = false) Integer idProducto) {
        return service.obtener(periodo, idProducto);
    }
}
