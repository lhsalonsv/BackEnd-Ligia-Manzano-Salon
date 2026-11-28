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
import java.util.Set;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ConfigContableService {

    private static final String ENTIDAD = "CONFIGURACION_CONTABLE";
    private static final Set<String> ACEPTAN_AGRUPADORAS = Set.of("ctaCapitalSocial", "ctaGastoIsr");

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
            String problema = codigo == null ? null : problemaDeCuenta(campo, codigo);
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

    private String problemaDeCuenta(String campo, String codigo) {
        if (!ACEPTAN_AGRUPADORAS.contains(campo)) return cuentas.problemaParaUsar(codigo);
        return cuentas.existe(codigo) ? null : "No existe esa cuenta.";
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
        c.put("ctaCaja", Texto.limpio(dto.ctaCaja()));
        c.put("ctaIngresoVentas", Texto.limpio(dto.ctaIngresoVentas()));
        c.put("ctaComprasProductos", Texto.limpio(dto.ctaComprasProductos()));
        c.put("ctaIvaDebitoCcf", Texto.limpio(dto.ctaIvaDebitoCcf()));
        c.put("ctaIvaDebitoCf", Texto.limpio(dto.ctaIvaDebitoCf()));
        c.put("ctaIvaCredito", Texto.limpio(dto.ctaIvaCredito()));
        c.put("ctaIvaPercibido", Texto.limpio(dto.ctaIvaPercibido()));
        c.put("ctaIvaRetenido", Texto.limpio(dto.ctaIvaRetenido()));
        c.put("ctaAnticipoIva", Texto.limpio(dto.ctaAnticipoIva()));
        c.put("ctaRemanenteIva", Texto.limpio(dto.ctaRemanenteIva()));
        c.put("ctaIvaPorPagar", Texto.limpio(dto.ctaIvaPorPagar()));
        c.put("ctaPagoCuenta", Texto.limpio(dto.ctaPagoCuenta()));
        c.put("ctaPagoCuentaPorPagar", Texto.limpio(dto.ctaPagoCuentaPorPagar()));
        c.put("ctaSueldos", Texto.limpio(dto.ctaSueldos()));
        c.put("ctaComisiones", Texto.limpio(dto.ctaComisiones()));
        c.put("ctaIsssLaboral", Texto.limpio(dto.ctaIsssLaboral()));
        c.put("ctaAfpLaboral", Texto.limpio(dto.ctaAfpLaboral()));
        c.put("ctaRentaRetenida", Texto.limpio(dto.ctaRentaRetenida()));
        c.put("ctaOtrosDescuentos", Texto.limpio(dto.ctaOtrosDescuentos()));
        c.put("ctaSalariosPorPagar", Texto.limpio(dto.ctaSalariosPorPagar()));
        c.put("ctaCapitalSocial", Texto.limpio(dto.ctaCapitalSocial()));
        c.put("ctaGastoIsr", Texto.limpio(dto.ctaGastoIsr()));
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
        c.setCtaCaja(cuentas.get("ctaCaja"));
        c.setCtaIngresoVentas(cuentas.get("ctaIngresoVentas"));
        c.setCtaComprasProductos(cuentas.get("ctaComprasProductos"));
        c.setCtaIvaDebitoCcf(cuentas.get("ctaIvaDebitoCcf"));
        c.setCtaIvaDebitoCf(cuentas.get("ctaIvaDebitoCf"));
        c.setCtaIvaCredito(cuentas.get("ctaIvaCredito"));
        c.setCtaIvaPercibido(cuentas.get("ctaIvaPercibido"));
        c.setCtaIvaRetenido(cuentas.get("ctaIvaRetenido"));
        c.setCtaAnticipoIva(cuentas.get("ctaAnticipoIva"));
        c.setCtaRemanenteIva(cuentas.get("ctaRemanenteIva"));
        c.setCtaIvaPorPagar(cuentas.get("ctaIvaPorPagar"));
        c.setCtaPagoCuenta(cuentas.get("ctaPagoCuenta"));
        c.setCtaPagoCuentaPorPagar(cuentas.get("ctaPagoCuentaPorPagar"));
        c.setCtaSueldos(cuentas.get("ctaSueldos"));
        c.setCtaComisiones(cuentas.get("ctaComisiones"));
        c.setCtaIsssLaboral(cuentas.get("ctaIsssLaboral"));
        c.setCtaAfpLaboral(cuentas.get("ctaAfpLaboral"));
        c.setCtaRentaRetenida(cuentas.get("ctaRentaRetenida"));
        c.setCtaOtrosDescuentos(cuentas.get("ctaOtrosDescuentos"));
        c.setCtaSalariosPorPagar(cuentas.get("ctaSalariosPorPagar"));
        c.setCtaCapitalSocial(cuentas.get("ctaCapitalSocial"));
        c.setCtaGastoIsr(cuentas.get("ctaGastoIsr"));
        c.setTipoPartidaVentas(dto.tipoPartidaVentas());
        c.setTipoPartidaCompras(dto.tipoPartidaCompras());
        c.setModoVentas(dto.modoVentas());
        c.setModoCompras(dto.modoCompras());
        c.setFechaInicio(dto.fechaInicio());
    }

    private static ConfigContableDTO convertirADTO(ConfigContableEntity c) {
        return new ConfigContableDTO(
                c.getCtaUtilidadEjercicio(),
                c.getCtaUtilidadesAnteriores(),
                c.getCtaPerdidaEjercicio(),
                c.getCtaPerdidasAnteriores(),
                c.getCtaPerdidasGanancias(),
                c.getCtaReservaLegal(),
                c.getCtaClientes(),
                c.getCtaProveedores(),
                c.getTipoPartidaVentas(),
                c.getTipoPartidaCompras(),
                c.getModoVentas(),
                c.getModoCompras(),
                c.getFechaInicio(),
                c.getCtaCaja(),
                c.getCtaIngresoVentas(),
                c.getCtaComprasProductos(),
                c.getCtaIvaDebitoCcf(),
                c.getCtaIvaDebitoCf(),
                c.getCtaIvaCredito(),
                c.getCtaIvaPercibido(),
                c.getCtaIvaRetenido(),
                c.getCtaAnticipoIva(),
                c.getCtaRemanenteIva(),
                c.getCtaIvaPorPagar(),
                c.getCtaPagoCuenta(),
                c.getCtaPagoCuentaPorPagar(),
                c.getCtaSueldos(),
                c.getCtaComisiones(),
                c.getCtaIsssLaboral(),
                c.getCtaAfpLaboral(),
                c.getCtaRentaRetenida(),
                c.getCtaOtrosDescuentos(),
                c.getCtaSalariosPorPagar(),
                c.getCtaCapitalSocial(),
                c.getCtaGastoIsr());
    }

    private static Map<String, Object> datosBitacora(ConfigContableDTO d) {
        Map<String, Object> datos = new LinkedHashMap<>(cuentasDe(d));
        datos.put("fechaInicio", d.fechaInicio());
        datos.put("tipoPartidaVentas", d.tipoPartidaVentas());
        datos.put("tipoPartidaCompras", d.tipoPartidaCompras());
        datos.put("modoVentas", d.modoVentas());
        datos.put("modoCompras", d.modoCompras());
        return datos;
    }
}
