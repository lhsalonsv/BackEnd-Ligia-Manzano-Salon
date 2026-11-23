package com.ligiamanzano.salon.configuracion.service;

import com.ligiamanzano.salon.configuracion.repository.ParametroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ParametroService {

    public static final String COMISION_SERVICIO_PCT = "COMISION_SERVICIO_PCT";
    public static final String COMISION_PRODUCTO = "COMISION_PRODUCTO";
    public static final String IVA_PCT = "IVA_PCT";
    public static final String ISSS_EMPLEADO_PCT = "ISSS_EMPLEADO_PCT";
    public static final String ISSS_TOPE_MENSUAL = "ISSS_TOPE_MENSUAL";
    public static final String AFP_EMPLEADO_PCT = "AFP_EMPLEADO_PCT";

    private final ParametroRepository repo;

    public BigDecimal numero(String clave) {
        return repo.findById(clave)
                .map(p -> new BigDecimal(p.getValor().trim()))
                .orElseThrow(() -> new IllegalStateException("Falta el parámetro " + clave + " en tb_parametros"));
    }
}
