package com.ligiamanzano.salon.contabilidad.controller;

import com.ligiamanzano.salon.contabilidad.dto.CuentaContableDTO;
import com.ligiamanzano.salon.contabilidad.dto.CuentaEdicionRequest;
import com.ligiamanzano.salon.contabilidad.dto.CuentaNuevaRequest;
import com.ligiamanzano.salon.contabilidad.service.CuentaContableService;
import com.ligiamanzano.salon.security.Permisos;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contabilidad/cuentas")
@PreAuthorize(Permisos.LECTURA_CONTABLE)
@RequiredArgsConstructor
public class CuentaContableController {

    private final CuentaContableService service;

    @GetMapping
    public List<CuentaContableDTO> listar() {
        return service.listar();
    }

    @PostMapping
    @PreAuthorize(Permisos.CONTABILIDAD)
    public ResponseEntity<CuentaContableDTO> crear(@Valid @RequestBody CuentaNuevaRequest dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(dto));
    }

    @PutMapping("/{codigo}")
    @PreAuthorize(Permisos.CONTABILIDAD)
    public CuentaContableDTO actualizar(@PathVariable String codigo, @Valid @RequestBody CuentaEdicionRequest dto) {
        return service.actualizar(codigo, dto);
    }

    @DeleteMapping("/{codigo}")
    @PreAuthorize(Permisos.CONTABILIDAD)
    public ResponseEntity<Void> eliminar(@PathVariable String codigo) {
        service.eliminar(codigo);
        return ResponseEntity.noContent().build();
    }
}
