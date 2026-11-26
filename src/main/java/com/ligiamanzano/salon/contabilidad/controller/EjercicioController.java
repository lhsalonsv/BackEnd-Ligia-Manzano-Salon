package com.ligiamanzano.salon.contabilidad.controller;

import com.ligiamanzano.salon.contabilidad.dto.EjercicioDTO;
import com.ligiamanzano.salon.contabilidad.dto.EjercicioRequest;
import com.ligiamanzano.salon.contabilidad.service.EjercicioService;
import com.ligiamanzano.salon.security.Permisos;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contabilidad")
@PreAuthorize(Permisos.LECTURA_CONTABLE)
@RequiredArgsConstructor
public class EjercicioController {

    private final EjercicioService service;

    @GetMapping("/ejercicios")
    public List<EjercicioDTO> listar() {
        return service.listar();
    }

    @PostMapping("/ejercicios")
    @PreAuthorize(Permisos.CONTABILIDAD)
    public ResponseEntity<EjercicioDTO> crear(@Valid @RequestBody EjercicioRequest dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(dto.anio()));
    }

    @PostMapping("/periodos/{anio}/{mes}/cerrar")
    @PreAuthorize(Permisos.CONTABILIDAD)
    public EjercicioDTO cerrar(@PathVariable short anio, @PathVariable short mes) {
        return service.cerrarPeriodo(anio, mes);
    }

    @PostMapping("/periodos/{anio}/{mes}/reabrir")
    @PreAuthorize(Permisos.CONTABILIDAD)
    public EjercicioDTO reabrir(@PathVariable short anio, @PathVariable short mes) {
        return service.reabrirPeriodo(anio, mes);
    }
}
