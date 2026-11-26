package com.ligiamanzano.salon.compras.service;

import com.ligiamanzano.salon.bitacora.service.BitacoraService;
import com.ligiamanzano.salon.common.exception.ConflictoException;
import com.ligiamanzano.salon.common.exception.ErrorDeBase;
import com.ligiamanzano.salon.common.exception.OperacionInvalidaException;
import com.ligiamanzano.salon.common.exception.RecursoNoEncontradoException;
import com.ligiamanzano.salon.common.response.PaginaDTO;
import com.ligiamanzano.salon.common.util.Texto;
import com.ligiamanzano.salon.compras.dto.*;
import com.ligiamanzano.salon.compras.entity.CompraEntity;
import com.ligiamanzano.salon.compras.entity.DetalleCompraEntity;
import com.ligiamanzano.salon.compras.entity.ProveedorEntity;
import com.ligiamanzano.salon.compras.repository.CompraConsultaRepository;
import com.ligiamanzano.salon.compras.repository.CompraConsultaRepository.Filtro;
import com.ligiamanzano.salon.compras.repository.CompraRepository;
import com.ligiamanzano.salon.compras.repository.DetalleCompraRepository;
import com.ligiamanzano.salon.configuracion.service.ParametroService;
import com.ligiamanzano.salon.inventario.service.InventarioService;
import com.ligiamanzano.salon.inventario.service.ProductoService;
import com.ligiamanzano.salon.security.UsuarioActual;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CompraService {

    private static final String ENTIDAD = "COMPRA";
    private static final String CCF = "CCF";
    private static final String NC = "NC";
    private static final String ND = "ND";
    private static final String OTRO = "OTRO";
    private static final Set<String> CON_IVA = Set.of(CCF, NC, ND);
    private static final String REGISTRADA = "REGISTRADA";
    private static final String ANULADA = "ANULADA";
    private static final short DTE = 4;
    private static final short IMPRESO = 1;
    private static final int MESES_PARA_DECLARAR = 3;
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    private static final BigDecimal PERCEPCION_PCT = BigDecimal.ONE;
    private static final Set<Short> OPERACIONES_RENTA = Set.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 8);
    private static final Set<Short> CLASIFICACIONES_RENTA = Set.of((short) 1, (short) 2, (short) 8);
    private static final Set<Short> SECTORES_RENTA = Set.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 8);

    private final CompraRepository repo;
    private final DetalleCompraRepository detalleRepo;
    private final CompraConsultaRepository consultas;
    private final ProveedorService proveedores;
    private final ProductoService productos;
    private final InventarioService inventario;
    private final ParametroService parametros;
    private final BitacoraService bitacora;

    public ComprasPaginaDTO listar(String buscar, String filtro, Integer idProveedor, int pagina, int tamano) {
        String texto = Texto.limpio(buscar);
        Filtro elegido = Filtro.desde(filtro);
        Map<String, Long> conteos = consultas.conteos(texto, idProveedor);
        List<CompraListaDTO> contenido = consultas.pagina(
                texto, idProveedor, elegido, Math.max(pagina, 0), PaginaDTO.tamanoSeguro(tamano));
        return new ComprasPaginaDTO(contenido, conteos.get(elegido.clave()), conteos, consultas.delMes());
    }

    public CompraDetalleDTO obtener(int idCompra) {
        return consultas.detalle(idCompra)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la compra."));
    }

    @Transactional
    public CompraDetalleDTO registrar(CompraRequest dto) {
        String numeroDocumento = Texto.limpio(dto.numeroDocumentoProveedor());
        UUID codigoGeneracion = dto.codigoGeneracionProveedor() == null ? null : UUID.fromString(dto.codigoGeneracionProveedor());
        ProveedorEntity proveedor = validar(dto, numeroDocumento, codigoGeneracion);
        boolean conIva = CON_IVA.contains(dto.tipoDocumentoProveedor());

        List<BigDecimal> subtotales = dto.detalle().stream()
                .map(l -> l.costoUnitario().multiply(BigDecimal.valueOf(l.cantidad())).setScale(2, RoundingMode.HALF_UP))
                .toList();
        BigDecimal gravadas = subtotales.stream().reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        BigDecimal exentas = dto.comprasExentas() == null ? BigDecimal.ZERO.setScale(2) : dto.comprasExentas().setScale(2);
        BigDecimal iva = conIva ? porcentaje(gravadas, parametros.numero(ParametroService.IVA_PCT)) : BigDecimal.ZERO.setScale(2);
        BigDecimal percibido = conIva && Boolean.TRUE.equals(dto.percepcion())
                ? porcentaje(gravadas, PERCEPCION_PCT)
                : BigDecimal.ZERO.setScale(2);

        CompraEntity c = new CompraEntity();
        c.setNumeroCompra(consultas.siguienteNumero());
        c.setIdProveedor(proveedor.getIdProveedor());
        c.setFechaCompra(dto.fechaCompra());
        c.setTipoDocumentoProveedor(dto.tipoDocumentoProveedor());
        c.setNumeroDocumentoProveedor(numeroDocumento);
        c.setClaseDocumento(dto.claseDocumento());
        c.setCodigoGeneracionProveedor(codigoGeneracion);
        c.setIdCompraRelacionada(dto.idCompraRelacionada());
        c.setPeriodoIva(dto.periodoIva());
        c.setSubtotalSinIva(gravadas);
        c.setComprasExentas(exentas);
        c.setIva(iva);
        c.setIvaPercibido(percibido);
        c.setTotal(gravadas.add(exentas).add(iva).add(percibido));
        c.setTipoOperacionRenta(dto.tipoOperacionRenta());
        c.setClasificacionRenta(dto.clasificacionRenta());
        c.setSectorRenta(dto.sectorRenta());
        c.setTipoCostoGasto(dto.tipoCostoGasto());
        c.setFechaVencimientoPago(proveedor.getDiasCredito() > 0
                ? dto.fechaCompra().plusDays(proveedor.getDiasCredito())
                : null);
        c.setEstado(REGISTRADA);
        c.setIdUsuario(UsuarioActual.id());
        repo.save(c);

        int lineasDeProducto = 0;
        for (int i = 0; i < dto.detalle().size(); i++) {
            LineaCompraRequest linea = dto.detalle().get(i);
            DetalleCompraEntity d = new DetalleCompraEntity();
            d.setIdCompra(c.getIdCompra());
            d.setIdProducto(linea.idProducto());
            d.setDescripcion(linea.idProducto() == null ? Texto.limpio(linea.descripcion()) : null);
            d.setCantidad(linea.cantidad());
            d.setCostoUnitario(linea.costoUnitario());
            d.setSubtotal(subtotales.get(i));
            detalleRepo.save(d);
            if (linea.idProducto() != null) {
                lineasDeProducto++;
                if (!NC.equals(c.getTipoDocumentoProveedor())) {
                    inventario.entrada(linea.idProducto(), InventarioService.ENTRADA_COMPRA, linea.cantidad(),
                            linea.costoUnitario(), d.getIdDetalleCompra(), null);
                }
            }
        }
        if (NC.equals(c.getTipoDocumentoProveedor()) && lineasDeProducto > 0) {
            registrarDevolucion(c.getIdCompra());
        }

        int gastos = dto.detalle().size() - lineasDeProducto;
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("tipoDocumentoProveedor", c.getTipoDocumentoProveedor());
        datos.put("total", c.getTotal());
        datos.put("productos", lineasDeProducto);
        datos.put("gastos", gastos);
        bitacora.registrarCambios(BitacoraService.COMPRAS, "REGISTRAR", ENTIDAD, c.getNumeroCompra(),
                describir(c, proveedor, lineasDeProducto, gastos), null, datos);
        return obtener(c.getIdCompra());
    }

    private ProveedorEntity validar(CompraRequest dto, String numeroDocumento, UUID codigoGeneracion) {
        Map<String, String> campos = new LinkedHashMap<>();
        String tipo = dto.tipoDocumentoProveedor();
        boolean nota = NC.equals(tipo) || ND.equals(tipo);

        Optional<ProveedorEntity> proveedor = proveedores.buscarParaCompra(dto.idProveedor());
        if (proveedor.isEmpty()) {
            campos.put("idProveedor", "Ese proveedor no existe.");
        } else if (!proveedor.get().isActivo()) {
            campos.put("idProveedor", "Ese proveedor está inactivo.");
        }
        if (numeroDocumento == null && !OTRO.equals(tipo)) {
            campos.put("numeroDocumentoProveedor", "Escribe el número de control del documento.");
        }
        if (dto.claseDocumento() != DTE && dto.claseDocumento() != IMPRESO) {
            campos.put("claseDocumento", "Elige DTE o impreso.");
        }
        if (CON_IVA.contains(tipo) && dto.claseDocumento() == DTE && codigoGeneracion == null) {
            campos.put("codigoGeneracionProveedor", "Escribe el código de generación del DTE (36 caracteres).");
        }
        String problemaRelacionada = revisarRelacionada(nota, dto.idCompraRelacionada(), dto.idProveedor());
        if (problemaRelacionada != null) campos.put("idCompraRelacionada", problemaRelacionada);
        String problemaPeriodo = revisarPeriodo(dto.periodoIva(), dto.fechaCompra(), CON_IVA.contains(tipo));
        if (problemaPeriodo != null) campos.put("periodoIva", problemaPeriodo);
        if (!OPERACIONES_RENTA.contains(dto.tipoOperacionRenta())) campos.put("tipoOperacionRenta", "Código no válido.");
        if (!CLASIFICACIONES_RENTA.contains(dto.clasificacionRenta())) campos.put("clasificacionRenta", "Código no válido.");
        if (!SECTORES_RENTA.contains(dto.sectorRenta())) campos.put("sectorRenta", "Código no válido.");
        if (dto.tipoCostoGasto() < 1 || dto.tipoCostoGasto() > 8) campos.put("tipoCostoGasto", "Código no válido.");
        String errorLinea = revisarLineas(dto.detalle(), tipo, problemaRelacionada == null ? dto.idCompraRelacionada() : null);
        if (errorLinea != null) campos.put("detalle", errorLinea);
        if (!campos.isEmpty()) {
            throw new OperacionInvalidaException("Revisa los campos marcados", "DATOS_INVALIDOS", campos);
        }

        Optional<CompraEntity> repetida = Optional.empty();
        if (numeroDocumento != null) {
            repetida = repo.findFirstByIdProveedorAndNumeroDocumentoProveedorIgnoreCaseAndEstadoNot(
                    dto.idProveedor(), numeroDocumento, ANULADA);
        }
        if (repetida.isEmpty() && codigoGeneracion != null) {
            repetida = repo.findFirstByCodigoGeneracionProveedorAndEstadoNot(codigoGeneracion, ANULADA);
        }
        repetida.ifPresent(otra -> {
            throw new ConflictoException(
                    "Ese documento ya se registró en la compra " + otra.getNumeroCompra() + ".",
                    "COMPRA_DUPLICADA",
                    Map.of("numeroDocumentoProveedor", "Ya está registrado."),
                    Map.of("idCompraExistente", otra.getIdCompra()));
        });
        return proveedor.get();
    }

    private String revisarRelacionada(boolean nota, Integer idCompraRelacionada, Integer idProveedor) {
        if (!nota) {
            return idCompraRelacionada == null ? null : "Solo una nota de crédito o débito corrige otra compra.";
        }
        if (idCompraRelacionada == null) return "Elige la compra que corrige.";
        CompraEntity relacionada = repo.findById(idCompraRelacionada).orElse(null);
        if (relacionada == null || !relacionada.getIdProveedor().equals(idProveedor)
                || !CCF.equals(relacionada.getTipoDocumentoProveedor()) || ANULADA.equals(relacionada.getEstado())) {
            return "Elige un CCF registrado de este mismo proveedor.";
        }
        return null;
    }

    private static String revisarPeriodo(LocalDate periodo, LocalDate fecha, boolean conIva) {
        if (fecha == null) return null;
        LocalDate mes = fecha.withDayOfMonth(1);
        if (!conIva) {
            return mes.equals(periodo) ? null : "Sin IVA aparte, se declara en el mes de la fecha del documento.";
        }
        boolean primerDia = periodo.getDayOfMonth() == 1;
        boolean enRango = !periodo.isBefore(mes) && !periodo.isAfter(mes.plusMonths(MESES_PARA_DECLARAR));
        return primerDia && enRango ? null : "El crédito fiscal se declara en el mes del documento o hasta 3 meses después.";
    }

    private String revisarLineas(List<LineaCompraRequest> lineas, String tipo, Integer idCompraRelacionada) {
        Map<Integer, Integer> vistos = new HashMap<>();
        for (int i = 0; i < lineas.size(); i++) {
            LineaCompraRequest linea = lineas.get(i);
            String inicio = "La línea " + (i + 1) + ": ";
            if (linea.idProducto() == null) {
                if (Texto.limpio(linea.descripcion()) == null) return inicio + "escribe la descripción del gasto.";
                if (linea.cantidad() != 1) return inicio + "un gasto lleva cantidad 1.";
                continue;
            }
            if (ND.equals(tipo)) return inicio + "una nota de débito no lleva productos.";
            Integer anterior = vistos.putIfAbsent(linea.idProducto(), i + 1);
            if (anterior != null) return inicio + "ese producto ya está en la línea " + anterior + ".";
            String problema = productos.problemaParaComprar(linea.idProducto());
            if (problema != null) return inicio + problema;
            if (NC.equals(tipo) && idCompraRelacionada != null) {
                String devolucion = revisarDevolucion(linea, idCompraRelacionada);
                if (devolucion != null) return inicio + devolucion;
            }
        }
        return null;
    }

    private String revisarDevolucion(LineaCompraRequest linea, int idCompraRelacionada) {
        CompraEntity relacionada = repo.findById(idCompraRelacionada).orElseThrow();
        String nombre = productos.nombre(linea.idProducto());
        DetalleCompraEntity comprada = detalleRepo.findAllByIdCompra(idCompraRelacionada).stream()
                .filter(d -> linea.idProducto().equals(d.getIdProducto()))
                .findFirst()
                .orElse(null);
        if (comprada == null) {
            return nombre + " no viene en la compra " + relacionada.getNumeroCompra() + ".";
        }
        if (linea.cantidad() > comprada.getCantidad()) {
            return nombre + ": la compra " + relacionada.getNumeroCompra() + " trajo " + comprada.getCantidad()
                    + ", no se pueden devolver " + linea.cantidad() + ".";
        }
        return null;
    }

    private void registrarDevolucion(int idCompra) {
        try {
            consultas.registrarDevolucion(idCompra, UsuarioActual.id());
        } catch (DataAccessException e) {
            String mensaje = ErrorDeBase.mensaje(e).orElseThrow(() -> e);
            throw new ConflictoException(mensaje, "STOCK_INSUFICIENTE", null);
        }
    }

    private static BigDecimal porcentaje(BigDecimal monto, BigDecimal pct) {
        return monto.multiply(pct).divide(CIEN, 2, RoundingMode.HALF_UP);
    }

    private String describir(CompraEntity c, ProveedorEntity proveedor, int productos, int gastos) {
        String total = " por $" + c.getTotal().toPlainString();
        String quien = " de " + ProveedorService.nombre(proveedor);
        return switch (c.getTipoDocumentoProveedor()) {
            case NC, ND -> "Registró la nota de " + (NC.equals(c.getTipoDocumentoProveedor()) ? "crédito " : "débito ")
                    + c.getNumeroCompra() + quien + total + ", que corrige "
                    + repo.findById(c.getIdCompraRelacionada()).map(CompraEntity::getNumeroCompra).orElse("otra compra")
                    + (productos > 0 ? ", con devolución de " + cuenta(productos, 0) : "");
            default -> "Registró la compra " + c.getNumeroCompra() + quien + total + " (" + cuenta(productos, gastos) + ")";
        };
    }

    private static String cuenta(int productos, int gastos) {
        List<String> partes = new ArrayList<>();
        if (productos > 0) partes.add(productos == 1 ? "1 producto" : productos + " productos");
        if (gastos > 0) partes.add(gastos == 1 ? "1 gasto" : gastos + " gastos");
        return String.join(", ", partes);
    }
}
