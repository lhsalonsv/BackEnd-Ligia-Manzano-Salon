package com.ligiamanzano.salon.compras.service;

import com.ligiamanzano.salon.bitacora.service.BitacoraService;
import com.ligiamanzano.salon.common.exception.ConflictoException;
import com.ligiamanzano.salon.common.exception.OperacionInvalidaException;
import com.ligiamanzano.salon.common.exception.RecursoNoEncontradoException;
import com.ligiamanzano.salon.common.util.Texto;
import com.ligiamanzano.salon.compras.dto.LiquidacionTarjetaDTO;
import com.ligiamanzano.salon.compras.dto.LiquidacionTarjetaRequest;
import com.ligiamanzano.salon.compras.dto.LiquidacionesTarjetaDTO;
import com.ligiamanzano.salon.compras.entity.CompraEntity;
import com.ligiamanzano.salon.compras.entity.LiquidacionTarjetaEntity;
import com.ligiamanzano.salon.compras.entity.ProveedorEntity;
import com.ligiamanzano.salon.compras.repository.CompraRepository;
import com.ligiamanzano.salon.compras.repository.LiquidacionTarjetaConsultaRepository;
import com.ligiamanzano.salon.compras.repository.LiquidacionTarjetaRepository;
import com.ligiamanzano.salon.contabilidad.service.CuentaContableService;
import com.ligiamanzano.salon.security.UsuarioActual;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class LiquidacionTarjetaService {

    private static final String ENTIDAD = "LIQUIDACION_TARJETA";
    private static final String ANULADA = "ANULADA";

    private final LiquidacionTarjetaRepository repo;
    private final LiquidacionTarjetaConsultaRepository consultas;
    private final ProveedorService proveedores;
    private final CompraRepository compras;
    private final CuentaContableService cuentas;
    private final BitacoraService bitacora;

    public LiquidacionesTarjetaDTO listar(LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            throw new OperacionInvalidaException("La fecha final es anterior a la inicial", "DATOS_INVALIDOS",
                    Map.of("hasta", "Tiene que ser igual o después de la fecha inicial."));
        }
        return new LiquidacionesTarjetaDTO(consultas.lista(desde, hasta));
    }

    @Transactional
    public LiquidacionTarjetaDTO crear(LiquidacionTarjetaRequest dto) {
        ProveedorEntity banco = validar(dto, null);
        LiquidacionTarjetaEntity t = new LiquidacionTarjetaEntity();
        convertirAEntity(dto, t);
        t.setIdUsuario(UsuarioActual.id());
        repo.saveAndFlush(t);
        bitacora.registrarCambios(BitacoraService.COMPRAS, "CREAR", ENTIDAD, t.getNumeroDocumento(),
                "Registró la liquidación " + t.getNumeroDocumento() + " de " + ProveedorService.nombre(banco)
                        + " por $" + t.getMontoLiquidado().toPlainString(),
                null, datosBitacora(t));
        return obtener(t.getIdLiquidacion());
    }

    @Transactional
    public LiquidacionTarjetaDTO actualizar(int idLiquidacion, LiquidacionTarjetaRequest dto) {
        LiquidacionTarjetaEntity t = buscar(idLiquidacion);
        validar(dto, idLiquidacion);
        Map<String, Object> antes = datosBitacora(t);
        convertirAEntity(dto, t);
        repo.saveAndFlush(t);
        bitacora.registrarCambios(BitacoraService.COMPRAS, "EDITAR", ENTIDAD, t.getNumeroDocumento(),
                "Editó la liquidación " + t.getNumeroDocumento(), antes, datosBitacora(t));
        return obtener(idLiquidacion);
    }

    @Transactional
    public void borrar(int idLiquidacion) {
        LiquidacionTarjetaEntity t = buscar(idLiquidacion);
        Map<String, Object> antes = datosBitacora(t);
        repo.delete(t);
        repo.flush();
        bitacora.registrarCambios(BitacoraService.COMPRAS, "BORRAR", ENTIDAD, t.getNumeroDocumento(),
                "Borró la liquidación " + t.getNumeroDocumento(), antes, null);
    }

    private LiquidacionTarjetaDTO obtener(int idLiquidacion) {
        return consultas.una(idLiquidacion).orElseThrow();
    }

    private LiquidacionTarjetaEntity buscar(int idLiquidacion) {
        return repo.findById(idLiquidacion)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la liquidación."));
    }

    private ProveedorEntity validar(LiquidacionTarjetaRequest dto, Integer idLiquidacion) {
        Map<String, String> campos = new LinkedHashMap<>();
        Optional<ProveedorEntity> banco = proveedores.buscarParaCompra(dto.idProveedor());
        if (banco.isEmpty()) {
            campos.put("idProveedor", "Ese proveedor no existe.");
        } else if (!banco.get().isActivo()) {
            campos.put("idProveedor", "Ese proveedor está inactivo.");
        }
        if (dto.periodoIva().getDayOfMonth() != 1) {
            campos.put("periodoIva", "Es el primer día del mes en que se declara (AAAA-MM-01).");
        }
        String problemaCuenta = cuentas.problemaParaRegistrar(dto.codigoCuentaBanco().trim());
        if (problemaCuenta != null) campos.put("codigoCuentaBanco", problemaCuenta);
        if (dto.codigoGeneracion() != null) {
            repo.findFirstByCodigoGeneracion(UUID.fromString(dto.codigoGeneracion()))
                    .filter(otra -> !otra.getIdLiquidacion().equals(idLiquidacion))
                    .ifPresent(otra -> campos.put("codigoGeneracion",
                            "Ya está registrado en la liquidación " + otra.getNumeroDocumento() + "."));
        }
        if (dto.idCompraComision() != null) {
            CompraEntity compra = compras.findById(dto.idCompraComision()).orElse(null);
            if (compra == null || ANULADA.equals(compra.getEstado()) || !compra.getIdProveedor().equals(dto.idProveedor())) {
                campos.put("idCompraComision", "Elige una compra registrada de este mismo banco.");
            }
        }
        if (!campos.isEmpty()) {
            throw new OperacionInvalidaException("Revisa los campos marcados", "DATOS_INVALIDOS", campos);
        }
        BigDecimal esperado = dto.montoLiquidado().subtract(cero(dto.anticipoIva())).subtract(cero(dto.comisionTotal()));
        if (esperado.compareTo(dto.montoDepositado()) != 0) {
            String mensaje = "El monto depositado tiene que ser lo liquidado menos el anticipo y la comisión: $"
                    + esperado.setScale(2).toPlainString() + ".";
            throw new ConflictoException(mensaje, "REGLA_DE_LA_BASE", Map.of("montoDepositado", mensaje));
        }
        return banco.get();
    }

    private static BigDecimal cero(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    private static void convertirAEntity(LiquidacionTarjetaRequest dto, LiquidacionTarjetaEntity t) {
        t.setFecha(dto.fecha());
        t.setIdProveedor(dto.idProveedor());
        t.setNumeroDocumento(dto.numeroDocumento().trim());
        t.setCodigoGeneracion(dto.codigoGeneracion() == null ? null : UUID.fromString(dto.codigoGeneracion()));
        t.setPeriodoIva(dto.periodoIva());
        t.setMontoLiquidado(dto.montoLiquidado().setScale(2));
        t.setAnticipoIva(cero(dto.anticipoIva()).setScale(2));
        t.setComisionTotal(cero(dto.comisionTotal()).setScale(2));
        t.setMontoDepositado(dto.montoDepositado().setScale(2));
        t.setCodigoCuentaBanco(Texto.limpio(dto.codigoCuentaBanco()));
        t.setIdCompraComision(dto.idCompraComision());
    }

    private static Map<String, Object> datosBitacora(LiquidacionTarjetaEntity t) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("fecha", t.getFecha());
        datos.put("idProveedor", t.getIdProveedor());
        datos.put("numeroDocumento", t.getNumeroDocumento());
        datos.put("codigoGeneracion", t.getCodigoGeneracion() == null ? null : t.getCodigoGeneracion().toString());
        datos.put("periodoIva", t.getPeriodoIva());
        datos.put("montoLiquidado", t.getMontoLiquidado());
        datos.put("anticipoIva", t.getAnticipoIva());
        datos.put("comisionTotal", t.getComisionTotal());
        datos.put("montoDepositado", t.getMontoDepositado());
        datos.put("codigoCuentaBanco", t.getCodigoCuentaBanco());
        datos.put("idCompraComision", t.getIdCompraComision());
        return datos;
    }
}
