package com.ligiamanzano.salon.inventario.service;

import com.ligiamanzano.salon.bitacora.service.BitacoraService;
import com.ligiamanzano.salon.common.exception.ConflictoException;
import com.ligiamanzano.salon.common.util.Texto;
import com.ligiamanzano.salon.inventario.dto.MarcaDTO;
import com.ligiamanzano.salon.inventario.dto.MarcaRequest;
import com.ligiamanzano.salon.inventario.entity.MarcaEntity;
import com.ligiamanzano.salon.inventario.repository.MarcaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MarcaService {

    private final MarcaRepository repo;
    private final BitacoraService bitacora;

    public List<MarcaDTO> listar() {
        return repo.findAllByOrderByNombreMarcaAsc().stream()
                .map(MarcaService::convertirADTO)
                .toList();
    }

    //Para que Productos valide pasando por el servicio dueño.
    public boolean existe(Integer idMarca) {
        return idMarca != null && repo.existsById(idMarca);
    }

    @Transactional
    public MarcaDTO crear(MarcaRequest dto) {
        String nombre = Texto.limpio(dto.nombreMarca());
        repo.buscarMismoNombre(nombre).ifPresent(otra -> {
            throw new ConflictoException("La marca " + otra.getNombreMarca() + " ya existe.", "MARCA_DUPLICADA",
                    Map.of("nombreMarca", "Ya existe esta marca."),
                    Map.of("idMarcaExistente", otra.getIdMarca()));
        });
        MarcaEntity m = new MarcaEntity();
        m.setNombreMarca(nombre);
        repo.save(m);
        bitacora.registrarCambios(BitacoraService.INVENTARIO, "CREAR", "MARCA", m.getIdMarca(),
                "Agregó la marca " + nombre, null, Map.of("nombreMarca", nombre));
        return convertirADTO(m);
    }

    private static MarcaDTO convertirADTO(MarcaEntity m) {
        return new MarcaDTO(m.getIdMarca(), m.getNombreMarca());
    }
}