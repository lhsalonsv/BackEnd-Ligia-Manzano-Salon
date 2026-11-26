package com.ligiamanzano.salon.contabilidad.service;

import com.ligiamanzano.salon.bitacora.service.BitacoraService;
import com.ligiamanzano.salon.common.exception.OperacionInvalidaException;
import com.ligiamanzano.salon.common.util.Texto;
import com.ligiamanzano.salon.contabilidad.dto.ConfigContableDTO;
import com.ligiamanzano.salon.contabilidad.entity.ConfigContableEntity;
import com.ligiamanzano.salon.contabilidad.repository.ConfigContableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ConfigContableService {

    private static final String ENTIDAD = "CONFIGURACION_CONTABLE";

    private final ConfigContableRepository repo;
    private final CuentaContableService cuentas;
    private final TipoPartidaService tipos;
    private final BitacoraService bitacora;

    public ConfigContableDTO obtener() {
        return convertirADTO(buscar());
    }

    @Transactional
    public ConfigContableDTO actualizar(ConfigContableDTO dto) {
        Map<String, String> cuentasNuevas = cuentasDe(dto);
        Map<String, String> campos = new LinkedHashMap<>();
        cuentasNuevas.forEach((campo, codigo) -> {
            String problema = codigo == null ? null : cuentas.problemaParaUsar(codigo);
            if (problema != null) campos.put(campo, problema);
        });
        if (!tipos.existeActivo(dto.tipoPartidaVentas())) campos.put("tipoPartidaVentas", "Elige un tipo de partida activo.");
        if (!tipos.existeActivo(dto.tipoPartidaCompras())) campos.put("tipoPartidaCompras", "Elige un tipo de partida activo.");
        if (!campos.isEmpty()) {
            throw new OperacionInvalidaException("Revisa los campos marcados", "DATOS_INVALIDOS", campos);
        }

        ConfigContableEntity c = buscar();
        ConfigContableDTO antes = convertirADTO(c);
        convertirAEntity(cuentasNuevas, dto, c);
        c.setActualizadoEn(Instant.now());
        repo.saveAndFlush(c);
        ConfigContableDTO despues = convertirADTO(c);
        bitacora.registrarCambios(BitacoraService.CONTABILIDAD, "EDITAR", ENTIDAD, ConfigContableEntity.UNICA,
                "Editó la configuración contable", datosBitacora(antes), datosBitacora(despues));
        return despues;
    }

    private ConfigContableEntity buscar() {
        return repo.findById(ConfigContableEntity.UNICA).orElseThrow();
    }

    private static Map<String, String> cuentasDe(ConfigContableDTO dto) {
        Map<String, String> c = new LinkedHashMap<>();
        c.put("ctaUtilidadEjercicio", Texto.limpio(dto.ctaUtilidadEjercicio()));
        c.put("ctaUtilidadesAnteriores", Texto.limpio(dto.ctaUtilidadesAnteriores()));
        c.put("ctaPerdidaEjercicio", Texto.limpio(dto.ctaPerdidaEjercicio()));
        c.put("ctaPerdidasAnteriores", Texto.limpio(dto.ctaPerdidasAnteriores()));
        c.put("ctaPerdidasGanancias", Texto.limpio(dto.ctaPerdidasGanancias()));
        c.put("ctaReservaLegal", Texto.limpio(dto.ctaReservaLegal()));
        c.put("ctaClientes", Texto.limpio(dto.ctaClientes()));
        c.put("ctaProveedores", Texto.limpio(dto.ctaProveedores()));
        return c;
    }

    private static void convertirAEntity(Map<String, String> cuentas, ConfigContableDTO dto, ConfigContableEntity c) {
        c.setCtaUtilidadEjercicio(cuentas.get("ctaUtilidadEjercicio"));
        c.setCtaUtilidadesAnteriores(cuentas.get("ctaUtilidadesAnteriores"));
        c.setCtaPerdidaEjercicio(cuentas.get("ctaPerdidaEjercicio"));
        c.setCtaPerdidasAnteriores(cuentas.get("ctaPerdidasAnteriores"));
        c.setCtaPerdidasGanancias(cuentas.get("ctaPerdidasGanancias"));
        c.setCtaReservaLegal(cuentas.get("ctaReservaLegal"));
        c.setCtaClientes(cuentas.get("ctaClientes"));
        c.setCtaProveedores(cuentas.get("ctaProveedores"));
        c.setTipoPartidaVentas(dto.tipoPartidaVentas());
        c.setTipoPartidaCompras(dto.tipoPartidaCompras());
        c.setModoVentas(dto.modoVentas());
        c.setModoCompras(dto.modoCompras());
    }

    private static ConfigContableDTO convertirADTO(ConfigContableEntity c) {
        return new ConfigContableDTO(c.getCtaUtilidadEjercicio(), c.getCtaUtilidadesAnteriores(), c.getCtaPerdidaEjercicio(),
                c.getCtaPerdidasAnteriores(), c.getCtaPerdidasGanancias(), c.getCtaReservaLegal(), c.getCtaClientes(),
                c.getCtaProveedores(), c.getTipoPartidaVentas(), c.getTipoPartidaCompras(), c.getModoVentas(), c.getModoCompras());
    }

    private static Map<String, Object> datosBitacora(ConfigContableDTO d) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("ctaUtilidadEjercicio", d.ctaUtilidadEjercicio());
        datos.put("ctaUtilidadesAnteriores", d.ctaUtilidadesAnteriores());
        datos.put("ctaPerdidaEjercicio", d.ctaPerdidaEjercicio());
        datos.put("ctaPerdidasAnteriores", d.ctaPerdidasAnteriores());
        datos.put("ctaPerdidasGanancias", d.ctaPerdidasGanancias());
        datos.put("ctaReservaLegal", d.ctaReservaLegal());
        datos.put("ctaClientes", d.ctaClientes());
        datos.put("ctaProveedores", d.ctaProveedores());
        datos.put("tipoPartidaVentas", d.tipoPartidaVentas());
        datos.put("tipoPartidaCompras", d.tipoPartidaCompras());
        datos.put("modoVentas", d.modoVentas());
        datos.put("modoCompras", d.modoCompras());
        return datos;
    }
}
