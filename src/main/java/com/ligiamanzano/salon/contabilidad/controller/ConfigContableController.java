package com.ligiamanzano.salon.contabilidad.controller;

import com.ligiamanzano.salon.contabilidad.dto.ConfigContableDTO;
import com.ligiamanzano.salon.contabilidad.service.ConfigContableService;
import com.ligiamanzano.salon.security.Permisos;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contabilidad/configuracion")
@PreAuthorize(Permisos.LECTURA_CONTABLE)
@RequiredArgsConstructor
public class ConfigContableController {

    private final ConfigContableService service;

    @GetMapping
    public ConfigContableDTO obtener() {
        return service.obtener();
    }

    @PutMapping
    @PreAuthorize(Permisos.CONTABILIDAD)
    public ConfigContableDTO actualizar(@Valid @RequestBody ConfigContableDTO dto) {
        return service.actualizar(dto);
    }
}
