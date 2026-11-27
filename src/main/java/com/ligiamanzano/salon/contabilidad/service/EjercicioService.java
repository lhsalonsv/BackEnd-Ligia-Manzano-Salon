package com.ligiamanzano.salon.contabilidad.service;

import com.ligiamanzano.salon.bitacora.service.BitacoraService;
import com.ligiamanzano.salon.common.exception.ConflictoException;
import com.ligiamanzano.salon.common.exception.RecursoNoEncontradoException;
import com.ligiamanzano.salon.contabilidad.dto.CierreEjercicioDTO;
import com.ligiamanzano.salon.contabilidad.dto.EjercicioDTO;
import com.ligiamanzano.salon.contabilidad.dto.PartidaDetalleDTO;
import com.ligiamanzano.salon.contabilidad.repository.EjercicioRepository;
import com.ligiamanzano.salon.security.UsuarioActual;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class EjercicioService {

    private static final String EJERCICIO = "EJERCICIO";
    private static final String PERIODO = "PERIODO_CONTABLE";

    private final EjercicioRepository repo;
    private final PartidaService partidas;
    private final BitacoraService bitacora;

    public List<EjercicioDTO> listar() {
        return repo.lista(null);
    }

    @Transactional
    public EjercicioDTO crear(short anio) {
        if (repo.existe(anio)) {
            throw new ConflictoException("Ya existe el ejercicio " + anio + ".", "EJERCICIO_DUPLICADO",
                    Map.of("anio", "Ya existe."));
        }
        repo.crear(anio);
        bitacora.registrar(BitacoraService.CONTABILIDAD, "CREAR", EJERCICIO, anio,
                "Creó el ejercicio " + anio + " con sus 12 meses abiertos");
        return obtener(anio);
    }

    @Transactional
    public EjercicioDTO cerrarPeriodo(short anio, short mes) {
        validarPeriodo(anio, mes);
        repo.cerrarPeriodo(anio, mes, UsuarioActual.id());
        bitacora.registrar(BitacoraService.CONTABILIDAD, "CERRAR_MES", PERIODO, mes + "/" + anio,
                "Cerró el mes " + mes + "/" + anio);
        return obtener(anio);
    }

    @Transactional
    public EjercicioDTO reabrirPeriodo(short anio, short mes) {
        validarPeriodo(anio, mes);
        repo.reabrirPeriodo(anio, mes);
        bitacora.registrar(BitacoraService.CONTABILIDAD, "REABRIR_MES", PERIODO, mes + "/" + anio,
                "Reabrió el mes " + mes + "/" + anio);
        return obtener(anio);
    }

    @Transactional
    public PartidaDetalleDTO generarPartidaCierre(short anio) {
        int idPartida = repo.generarPartidaCierre(anio, UsuarioActual.id());
        bitacora.registrar(BitacoraService.CONTABILIDAD, "GENERAR_CIERRE", EJERCICIO, anio,
                "Generó el borrador de la partida de cierre del ejercicio " + anio);
        return partidas.obtener(idPartida);
    }

    @Transactional
    public CierreEjercicioDTO cerrarEjercicio(short anio) {
        Integer idTraslado = repo.cerrarEjercicio(anio, UsuarioActual.id());
        bitacora.registrar(BitacoraService.CONTABILIDAD, "CERRAR_EJERCICIO", EJERCICIO, anio,
                "Cerró el ejercicio " + anio + (idTraslado == null ? " (sin traslado del resultado)" : ""));
        return new CierreEjercicioDTO(idTraslado);
    }

    private void validarPeriodo(short anio, short mes) {
        if (mes < 1 || mes > 12 || !repo.existe(anio)) {
            throw new RecursoNoEncontradoException("No existe el mes " + mes + "/" + anio + ".");
        }
    }

    private EjercicioDTO obtener(short anio) {
        return repo.lista(anio).getFirst();
    }
}
