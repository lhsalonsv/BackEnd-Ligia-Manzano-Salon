package com.ligiamanzano.salon.contabilidad.service;

import com.ligiamanzano.salon.bitacora.service.BitacoraService;
import com.ligiamanzano.salon.common.exception.ConflictoException;
import com.ligiamanzano.salon.common.exception.RecursoNoEncontradoException;
import com.ligiamanzano.salon.contabilidad.dto.EjercicioDTO;
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

    private void validarPeriodo(short anio, short mes) {
        if (mes < 1 || mes > 12 || !repo.existe(anio)) {
            throw new RecursoNoEncontradoException("No existe el mes " + mes + "/" + anio + ".");
        }
    }

    private EjercicioDTO obtener(short anio) {
        return repo.lista(anio).getFirst();
    }
}
