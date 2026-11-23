package com.ligiamanzano.salon.bitacora.controller;

import com.ligiamanzano.salon.bitacora.dto.BitacoraDetalleDTO;
import com.ligiamanzano.salon.bitacora.dto.BitacoraPaginaDTO;
import com.ligiamanzano.salon.bitacora.service.BitacoraService;
import com.ligiamanzano.salon.security.Permisos;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/bitacora")
@PreAuthorize(Permisos.AUDITORIA)
@RequiredArgsConstructor
public class BitacoraController {

    private final BitacoraService service;

    @GetMapping
    public BitacoraPaginaDTO listar(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                                    @RequestParam(required = false) String modulo,
                                    @RequestParam(required = false) String idUsuario,
                                    @RequestParam(required = false) String buscar,
                                    @RequestParam(defaultValue = "0") int pagina,
                                    @RequestParam(defaultValue = "100") int tamano) {
        return service.listar(fecha, modulo, idUsuario, buscar, pagina, tamano);
    }

    @GetMapping("/{id}")
    public BitacoraDetalleDTO obtener(@PathVariable long id) {
        return service.obtener(id);
    }
}
