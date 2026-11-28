package com.ligiamanzano.salon.contabilidad.service;

import com.ligiamanzano.salon.bitacora.service.BitacoraService;
import com.ligiamanzano.salon.common.exception.OperacionInvalidaException;
import com.ligiamanzano.salon.contabilidad.dto.ContabilizadoDTO;
import com.ligiamanzano.salon.contabilidad.dto.ContabilizarRequest;
import com.ligiamanzano.salon.contabilidad.dto.ImpuestosDTO;
import com.ligiamanzano.salon.contabilidad.dto.VistaPreviaDTO;
import com.ligiamanzano.salon.contabilidad.repository.ContabilizarRepository;
import com.ligiamanzano.salon.security.UsuarioActual;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ContabilizarService {

    private static final Map<String, String> ORIGENES = Map.of(
            "VENTAS", "las ventas",
            "COMPRAS", "las compras",
            "PLANILLA", "la planilla",
            "TARJETAS", "las liquidaciones de tarjeta");
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ContabilizarRepository repo;
    private final BitacoraService bitacora;

    public VistaPreviaDTO vistaPrevia(String origen, LocalDate desde, LocalDate hasta) {
        return new VistaPreviaDTO(repo.pendientes(validar(origen, desde, hasta), desde, hasta));
    }

    @Transactional
    public ContabilizadoDTO contabilizar(String origen, ContabilizarRequest dto) {
        String elegido = validar(origen, dto.desde(), dto.hasta());
        List<Integer> ids = repo.contabilizar(elegido, dto.desde(), dto.hasta(), UsuarioActual.id());
        if (!ids.isEmpty()) {
            Map<String, Object> datos = new LinkedHashMap<>();
            datos.put("origen", elegido);
            datos.put("desde", dto.desde());
            datos.put("hasta", dto.hasta());
            datos.put("ids", ids);
            bitacora.registrarCambios(BitacoraService.CONTABILIDAD, "CONTABILIZAR", "ORIGEN", elegido,
                    "Contabilizó " + ORIGENES.get(elegido) + " del " + dto.desde().format(DIA) + " al " + dto.hasta().format(DIA)
                            + ": " + (ids.size() == 1 ? "1 partida" : ids.size() + " partidas") + " en borrador",
                    null, datos);
        }
        return new ContabilizadoDTO(ids);
    }

    @Transactional
    public ImpuestosDTO generarImpuestos(short anio, short mes) {
        if (mes < 1 || mes > 12) {
            throw new OperacionInvalidaException("Revisa los campos marcados", "DATOS_INVALIDOS",
                    Map.of("mes", "El mes va de 1 a 12."));
        }
        int idPartida = repo.generarImpuestos(anio, mes, UsuarioActual.id());
        String periodo = "%02d/%d".formatted(mes, anio);
        bitacora.registrar(BitacoraService.CONTABILIDAD, "GENERAR_IMPUESTOS", "PERIODO_IVA", periodo,
                "Generó en borrador la liquidación de IVA y el pago a cuenta de " + periodo);
        return new ImpuestosDTO(idPartida);
    }

    private static String validar(String origen, LocalDate desde, LocalDate hasta) {
        String elegido = origen.toUpperCase(Locale.ROOT);
        Map<String, String> campos = new LinkedHashMap<>();
        if (!ORIGENES.containsKey(elegido)) campos.put("origen", "Elige VENTAS, COMPRAS, PLANILLA o TARJETAS.");
        if (hasta.isBefore(desde)) campos.put("hasta", "Tiene que ser igual o después de la fecha inicial.");
        if (!campos.isEmpty()) {
            throw new OperacionInvalidaException("Revisa los campos marcados", "DATOS_INVALIDOS", campos);
        }
        return elegido;
    }
}
