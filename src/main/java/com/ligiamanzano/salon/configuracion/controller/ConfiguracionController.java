package com.ligiamanzano.salon.configuracion.controller;

import com.ligiamanzano.salon.configuracion.dto.NegocioDTO;
import com.ligiamanzano.salon.configuracion.dto.NegocioRequest;
import com.ligiamanzano.salon.configuracion.dto.ReglasDTO;
import com.ligiamanzano.salon.configuracion.dto.ReglasRequest;
import com.ligiamanzano.salon.configuracion.service.ConfiguracionService;
import com.ligiamanzano.salon.configuracion.service.NegocioService;
import com.ligiamanzano.salon.security.Permisos;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/configuracion")
@PreAuthorize(Permisos.ADMIN)
@RequiredArgsConstructor
public class ConfiguracionController {

    private final NegocioService negocio;
    private final ConfiguracionService configuracion;

    @GetMapping("/negocio")
    public NegocioDTO obtenerNegocio() {
        return negocio.obtener();
    }

    @PutMapping("/negocio")
    public NegocioDTO actualizarNegocio(@Valid @RequestBody NegocioRequest dto) {
        return negocio.actualizar(dto);
    }

    @GetMapping("/reglas")
    public ReglasDTO obtenerReglas() {
        return configuracion.obtenerReglas();
    }

    @PutMapping("/reglas")
    public ReglasDTO actualizarReglas(@Valid @RequestBody ReglasRequest dto) {
        return configuracion.actualizarReglas(dto);
    }
}
