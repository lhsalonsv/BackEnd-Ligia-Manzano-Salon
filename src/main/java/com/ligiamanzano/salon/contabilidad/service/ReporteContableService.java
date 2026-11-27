package com.ligiamanzano.salon.contabilidad.service;

import com.ligiamanzano.salon.common.exception.OperacionInvalidaException;
import com.ligiamanzano.salon.common.util.Texto;
import com.ligiamanzano.salon.configuracion.service.EmisorService;
import com.ligiamanzano.salon.contabilidad.dto.*;
import com.ligiamanzano.salon.contabilidad.entity.CuentaContableEntity;
import com.ligiamanzano.salon.contabilidad.repository.CuentaContableRepository;
import com.ligiamanzano.salon.contabilidad.repository.ReporteContableRepository;
import com.ligiamanzano.salon.contabilidad.repository.ReporteContableRepository.FilaDiario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReporteContableService {

    private static final short NIVEL_MAXIMO = 6;

    private final ReporteContableRepository repo;
    private final CuentaContableRepository cuentas;
    private final EmisorService emisor;

    public SaldosDTO saldos(short anio, short mes, Short nivel) {
        short nivelElegido = nivel == null ? NIVEL_MAXIMO : nivel;
        Map<String, String> campos = new LinkedHashMap<>();
        if (anio < 2000 || anio > 2100) campos.put("anio", "El año va de 2000 a 2100.");
        if (mes < 1 || mes > 12) campos.put("mes", "El mes va de 1 a 12.");
        if (nivelElegido < 0 || nivelElegido > NIVEL_MAXIMO) campos.put("nivel", "El nivel va de 0 a 6.");
        if (!campos.isEmpty()) {
            throw new OperacionInvalidaException("Revisa los campos marcados", "DATOS_INVALIDOS", campos);
        }
        return new SaldosDTO(emisor.encabezado(), repo.saldos(anio, mes, nivelElegido));
    }

    public MayorDTO mayor(String cuenta, LocalDate desde, LocalDate hasta) {
        String codigo = cuenta.trim();
        List<FilaMayorDTO> filas = repo.mayor(codigo, desde, hasta);
        CuentaContableEntity c = cuentas.findById(codigo).orElseThrow();
        return new MayorDTO(emisor.encabezado(),
                new CuentaMayorDTO(c.getCodigo(), c.getNombre(), c.getNaturaleza(), c.isAceptaMovimiento()), filas);
    }

    public DiarioDTO diario(LocalDate desde, LocalDate hasta, String tipo) {
        if (hasta.isBefore(desde)) {
            throw new OperacionInvalidaException("La fecha final es anterior a la inicial", "DATOS_INVALIDOS",
                    Map.of("hasta", "Tiene que ser igual o después de la fecha inicial."));
        }
        Map<Integer, List<FilaDiario>> porPartida = new LinkedHashMap<>();
        for (FilaDiario f : repo.diario(desde, hasta, Texto.limpio(tipo))) {
            porPartida.computeIfAbsent(f.idPartida(), id -> new ArrayList<>()).add(f);
        }
        List<PartidaDiarioDTO> partidas = porPartida.values().stream().map(ReporteContableService::convertirADTO).toList();
        return new DiarioDTO(emisor.encabezado(), partidas,
                suma(partidas.stream().map(PartidaDiarioDTO::totalDebe).toList()),
                suma(partidas.stream().map(PartidaDiarioDTO::totalHaber).toList()));
    }

    private static PartidaDiarioDTO convertirADTO(List<FilaDiario> filas) {
        FilaDiario p = filas.getFirst();
        List<LineaDiarioDTO> lineas = filas.stream()
                .map(f -> new LineaDiarioDTO(f.linea(), f.codigoCuenta(), f.nombreCuenta(), f.conceptoLinea(), f.debe(), f.haber()))
                .toList();
        return new PartidaDiarioDTO(p.idPartida(), p.numero(), p.fecha(), p.codigoTipo(), p.tipo(), p.concepto(), lineas,
                suma(lineas.stream().map(LineaDiarioDTO::debe).toList()),
                suma(lineas.stream().map(LineaDiarioDTO::haber).toList()));
    }

    private static BigDecimal suma(List<BigDecimal> montos) {
        return montos.stream().reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
    }
}
