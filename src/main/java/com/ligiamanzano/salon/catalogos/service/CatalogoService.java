package com.ligiamanzano.salon.catalogos.service;

import com.ligiamanzano.salon.catalogos.dto.ActividadDTO;
import com.ligiamanzano.salon.catalogos.dto.DistritoDTO;
import com.ligiamanzano.salon.catalogos.dto.UnidadMedidaDTO;
import com.ligiamanzano.salon.catalogos.repository.ActividadRepository;
import com.ligiamanzano.salon.catalogos.repository.DistritoRepository;
import com.ligiamanzano.salon.catalogos.repository.UnidadMedidaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CatalogoService {

    private final ActividadRepository actividadRepo;
    private final DistritoRepository distritoRepo;
    private final UnidadMedidaRepository unidadMedidaRepo;

    public List<ActividadDTO> listarActividades() {
        return actividadRepo.findAllByOrderByDescripcionAsc().stream()
                .map(a -> new ActividadDTO(a.getCodActividad(), a.getDescripcion()))
                .toList();
    }

    public List<DistritoDTO> listarDistritos() {
        return distritoRepo.listarConMunicipioYDepartamento();
    }

    public List<UnidadMedidaDTO> listarUnidadesMedida() {
        return unidadMedidaRepo.findAllByOrderByCodUnidadMedidaAsc().stream()
                .map(u -> new UnidadMedidaDTO(u.getCodUnidadMedida(), u.getNombre()))
                .toList();
    }

    public boolean existeActividad(String codActividad) {
        return codActividad != null && actividadRepo.existsById(codActividad);
    }

    public boolean existeDistrito(Integer idDistrito) {
        return idDistrito != null && distritoRepo.existsById(idDistrito);
    }

    public boolean existeUnidadMedida(Short codUnidadMedida) {
        return codUnidadMedida != null && unidadMedidaRepo.existsById(codUnidadMedida);
    }
}
