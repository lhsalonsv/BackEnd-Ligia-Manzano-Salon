package com.ligiamanzano.salon.personal.controller;

import com.ligiamanzano.salon.personal.dto.EspecialidadDTO;
import com.ligiamanzano.salon.personal.service.EspecialidadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/especialidades")
@RequiredArgsConstructor
public class EspecialidadController {

    private final EspecialidadService service;

    @GetMapping
    public List<EspecialidadDTO> listar() {
        return service.listarConEmpleados();
    }
}