package com.ligiamanzano.salon.contabilidad.service;

import com.ligiamanzano.salon.bitacora.service.BitacoraService;
import com.ligiamanzano.salon.common.exception.ConflictoException;
import com.ligiamanzano.salon.common.exception.OperacionInvalidaException;
import com.ligiamanzano.salon.common.exception.RecursoNoEncontradoException;
import com.ligiamanzano.salon.common.util.Texto;
import com.ligiamanzano.salon.contabilidad.dto.TipoPartidaDTO;
import com.ligiamanzano.salon.contabilidad.entity.TipoPartidaEntity;
import com.ligiamanzano.salon.contabilidad.repository.TipoPartidaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TipoPartidaService {

    private static final String ENTIDAD = "TIPO_PARTIDA";

    private final TipoPartidaRepository repo;
    private final BitacoraService bitacora;

    public List<TipoPartidaDTO> listar() {
        return repo.findAllByOrderByCodigoAsc().stream().map(TipoPartidaService::convertirADTO).toList();
    }

    @Transactional
    public TipoPartidaDTO crear(TipoPartidaDTO dto) {
        if (repo.existsById(dto.codigo())) {
            throw new ConflictoException("Ya existe un tipo con ese código.", "TIPO_PARTIDA_DUPLICADO",
                    Map.of("codigo", "Ya existe un tipo con ese código."));
        }
        validar(dto, null);
        TipoPartidaEntity t = new TipoPartidaEntity();
        t.setCodigo(dto.codigo());
        guardar(t, dto);
        bitacora.registrarCambios(BitacoraService.CONTABILIDAD, "CREAR", ENTIDAD, t.getCodigo(),
                "Agregó el tipo de partida " + t.getCodigo() + " " + t.getNombre(), null, datosBitacora(t));
        return convertirADTO(t);
    }

    @Transactional
    public TipoPartidaDTO actualizar(String codigo, TipoPartidaDTO dto) {
        TipoPartidaEntity t = repo.findById(codigo)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe ese tipo de partida."));
        validar(dto, t);
        Map<String, Object> antes = datosBitacora(t);
        guardar(t, dto);
        bitacora.registrarCambios(BitacoraService.CONTABILIDAD, "EDITAR", ENTIDAD, codigo,
                "Editó el tipo de partida " + codigo + " " + t.getNombre(), antes, datosBitacora(t));
        return convertirADTO(t);
    }

    public boolean existeActivo(String codigo) {
        return codigo != null && repo.findById(codigo).map(TipoPartidaEntity::isActivo).orElse(false);
    }

    private void validar(TipoPartidaDTO dto, TipoPartidaEntity actual) {
        Map<String, String> campos = new LinkedHashMap<>();
        if (dto.predeterminado() && !dto.activo()) {
            campos.put("activo", "El tipo predeterminado no se puede desactivar.");
        }
        if (actual != null && actual.isPredeterminado() && !dto.predeterminado()) {
            campos.put("predeterminado", "Debe haber un tipo predeterminado: marque otro como predeterminado.");
        }
        if (!campos.isEmpty()) {
            throw new OperacionInvalidaException("Revisa los campos marcados", "DATOS_INVALIDOS", campos);
        }
    }

    private void guardar(TipoPartidaEntity t, TipoPartidaDTO dto) {
        if (dto.predeterminado() && !t.isPredeterminado()) {
            repo.findFirstByPredeterminadoTrue().ifPresent(anterior -> {
                anterior.setPredeterminado(false);
                repo.saveAndFlush(anterior);
            });
        }
        t.setNombre(Texto.limpio(dto.nombre()));
        t.setPredeterminado(dto.predeterminado());
        t.setActivo(dto.activo());
        repo.saveAndFlush(t);
    }

    private static TipoPartidaDTO convertirADTO(TipoPartidaEntity t) {
        return new TipoPartidaDTO(t.getCodigo(), t.getNombre(), t.isPredeterminado(), t.isActivo());
    }

    private static Map<String, Object> datosBitacora(TipoPartidaEntity t) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("nombre", t.getNombre());
        datos.put("predeterminado", t.isPredeterminado());
        datos.put("activo", t.isActivo());
        return datos;
    }
}
