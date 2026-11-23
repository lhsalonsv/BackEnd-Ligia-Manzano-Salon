package com.ligiamanzano.salon.configuracion.service;

import com.ligiamanzano.salon.configuracion.dto.HorarioSalonDTO;
import com.ligiamanzano.salon.configuracion.repository.HorarioSalonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ConfiguracionService {

    private final HorarioSalonRepository horarioRepo;

    public List<HorarioSalonDTO> listarHorarioSalon() {
        return horarioRepo.findAllByOrderByDiaSemanaAsc().stream()
                .map(h -> new HorarioSalonDTO(h.getDiaSemana(), h.isAbierto(), h.getHoraAbre(), h.getHoraCierra()))
                .toList();
    }
}