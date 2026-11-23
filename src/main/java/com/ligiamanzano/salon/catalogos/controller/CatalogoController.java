package com.ligiamanzano.salon.catalogos.controller;

import com.ligiamanzano.salon.catalogos.dto.ActividadDTO;
import com.ligiamanzano.salon.catalogos.dto.DistritoDTO;
import com.ligiamanzano.salon.catalogos.dto.UnidadMedidaDTO;
import com.ligiamanzano.salon.catalogos.service.CatalogoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/catalogos")
@RequiredArgsConstructor
public class CatalogoController {

    private final CatalogoService service;

    @GetMapping("/actividades")
    public List<ActividadDTO> actividades() {
        return service.listarActividades();
    }

    @GetMapping("/distritos")
    public List<DistritoDTO> distritos() {
        return service.listarDistritos();
    }

    @GetMapping("/unidades-medida")
    public List<UnidadMedidaDTO> unidadesMedida() {
        return service.listarUnidadesMedida();
    }
}
