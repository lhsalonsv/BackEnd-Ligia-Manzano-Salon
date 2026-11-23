package com.ligiamanzano.salon.personal.service;

import com.ligiamanzano.salon.personal.dto.EspecialidadDTO;
import com.ligiamanzano.salon.personal.repository.EmpleadoConsultaRepository;
import com.ligiamanzano.salon.personal.repository.EspecialidadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class EspecialidadService {

    private final EmpleadoConsultaRepository consultas;
    private final EspecialidadRepository repo;

    public List<EspecialidadDTO> listarConEmpleados() {
        return consultas.especialidadesConEmpleados();
    }

    public boolean existe(Integer idEspecialidad) {
        return idEspecialidad != null && repo.existsById(idEspecialidad);
    }
}
