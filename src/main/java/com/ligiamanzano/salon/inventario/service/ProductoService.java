package com.ligiamanzano.salon.inventario.service;

import com.ligiamanzano.salon.bitacora.service.BitacoraService;
import com.ligiamanzano.salon.catalogos.service.CatalogoService;
import com.ligiamanzano.salon.common.exception.ConflictoException;
import com.ligiamanzano.salon.common.exception.OperacionInvalidaException;
import com.ligiamanzano.salon.common.exception.RecursoNoEncontradoException;
import com.ligiamanzano.salon.common.response.PaginaDTO;
import com.ligiamanzano.salon.common.util.Texto;
import com.ligiamanzano.salon.configuracion.service.ParametroService;
import com.ligiamanzano.salon.inventario.dto.*;
import com.ligiamanzano.salon.inventario.entity.ProductoEntity;
import com.ligiamanzano.salon.inventario.repository.ProductoConsultaRepository;
import com.ligiamanzano.salon.inventario.repository.ProductoConsultaRepository.Filtro;
import com.ligiamanzano.salon.inventario.repository.ProductoConsultaRepository.Nombres;
import com.ligiamanzano.salon.inventario.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProductoService {

    private static final String ENTIDAD = "PRODUCTO";
    private static final int ULTIMOS_MOVIMIENTOS = 5;
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final ProductoRepository repo;
    private final ProductoConsultaRepository consultas;
    private final InventarioService inventario;
    private final CategoriaService categorias;
    private final MarcaService marcas;
    private final CatalogoService catalogos;
    private final ParametroService parametros;
    private final BitacoraService bitacora;

    public ProductosPaginaDTO listar(String buscar, String filtro, Integer idCategoria, int pagina, int tamano) {
        String texto = Texto.limpio(buscar);
        Filtro elegido = Filtro.desde(filtro);
        Map<String, Long> conteos = consultas.conteos(texto, idCategoria);
        List<ProductoListaDTO> contenido = consultas.pagina(
                texto, idCategoria, elegido, Math.max(pagina, 0), PaginaDTO.tamanoSeguro(tamano));
        return new ProductosPaginaDTO(contenido, conteos.get(elegido.clave()), conteos, consultas.valorTotal(),
                parametros.numero(ParametroService.COMISION_PRODUCTO), parametros.numero(ParametroService.IVA_PCT));
    }

    public ProductoDetalleDTO obtener(int idProducto) {
        return convertirADetalle(buscar(idProducto));
    }

    //Lo usa Compras para validar cada línea de producto (que exista y esté activo)
    public String problemaParaComprar(Integer idProducto) {
        if (idProducto == null) return "falta el producto.";
        ProductoEntity p = repo.findById(idProducto).orElse(null);
        if (p == null) return "ese producto no existe.";
        return p.isActivo() ? null : "el producto " + p.getNombreProducto() + " está inactivo.";
    }

    public String nombre(int idProducto) {
        return repo.findById(idProducto).map(ProductoEntity::getNombreProducto).orElse("El producto " + idProducto);
    }

    @Transactional
    public ProductoDetalleDTO crear(ProductoRequest dto) {
        validar(dto, null);
        ProductoEntity p = new ProductoEntity();
        convertirAEntity(dto, p);
        repo.save(p);
        bitacora.registrarCambios(BitacoraService.INVENTARIO, "CREAR", ENTIDAD, p.getIdProducto(),
                "Creó el producto " + p.getNombreProducto(), null, datosBitacora(p));
        return convertirADetalle(p);
    }

    @Transactional
    public ProductoDetalleDTO actualizar(int idProducto, ProductoRequest dto) {
        ProductoEntity p = buscar(idProducto);
        validar(dto, idProducto);
        Map<String, Object> antes = datosBitacora(p);
        convertirAEntity(dto, p);
        bitacora.registrarCambios(BitacoraService.INVENTARIO, "EDITAR", ENTIDAD, idProducto,
                "Editó el producto " + p.getNombreProducto(), antes, datosBitacora(p));
        return convertirADetalle(p);
    }

    @Transactional
    public ProductoDetalleDTO ajustar(int idProducto, AjusteRequest dto) {
        ProductoEntity p = buscar(idProducto);
        String motivo = Texto.limpio(dto.motivo());
        int antes = inventario.existencia(idProducto);

        switch (dto.tipo()) {
            case "DANADO", "USO_SALON" -> {
                if (dto.cantidad() < 1) throw errorDeCampo("cantidad", "Tiene que ser al menos 1.");
                String tipo = "DANADO".equals(dto.tipo()) ? InventarioService.SALIDA_AJUSTE : InventarioService.USO_SALON;
                inventario.consumir(idProducto, dto.cantidad(), tipo, motivo, null);
            }
            case "CONTEO" -> {
                int diferencia = dto.cantidad() - antes;
                if (diferencia == 0) {
                    throw errorDeCampo("cantidad", "Ya hay " + antes + ": no hay nada que ajustar.");
                }
                if (diferencia < 0) {
                    inventario.consumir(idProducto, -diferencia, InventarioService.SALIDA_AJUSTE, motivo, null);
                } else {
                    BigDecimal costo = inventario.ultimoCosto(idProducto).orElseThrow(() -> errorDeCampo("cantidad",
                            "Este producto nunca se compró y no hay costo para la entrada. Registra una compra."));
                    inventario.entrada(idProducto, InventarioService.ENTRADA_AJUSTE, diferencia, costo, null, motivo);
                }
            }
            default -> throw errorDeCampo("tipo", "Tipo de ajuste no válido.");
        }

        int despues = inventario.existencia(idProducto);
        Map<String, Object> datosDespues = new LinkedHashMap<>();
        datosDespues.put("existencia", despues);
        datosDespues.put("motivo", motivo);
        bitacora.registrarCambios(BitacoraService.INVENTARIO, "AJUSTAR", ENTIDAD, idProducto,
                "Ajustó " + p.getNombreProducto() + ": " + conSigno(despues - antes) + " (" + textoTipo(dto.tipo()) + ")",
                Map.of("existencia", antes), datosDespues);
        return convertirADetalle(p);
    }

    private ProductoEntity buscar(int idProducto) {
        return repo.findById(idProducto)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el producto."));
    }

    private void validar(ProductoRequest dto, Integer idActual) {
        Map<String, String> campos = new LinkedHashMap<>();
        if (!marcas.existe(dto.idMarca())) campos.put("idMarca", "Esa marca no existe.");
        if (!categorias.existe(dto.idCategoria())) campos.put("idCategoria", "Esa categoría no existe.");
        if (!catalogos.existeUnidadMedida(dto.codUnidadMedida())) {
            campos.put("codUnidadMedida", "No está en el catálogo de Hacienda.");
        }
        BigDecimal ivaPct = parametros.numero(ParametroService.IVA_PCT);
        BigDecimal sinIva = dto.precioVenta().divide(BigDecimal.ONE.add(ivaPct.divide(CIEN)), 2, RoundingMode.HALF_UP);
        if (dto.comisionFija().compareTo(sinIva) > 0) {
            campos.put("comisionFija", "No puede ser mayor que el precio sin IVA ($" + sinIva.toPlainString() + ").");
        }
        if (!campos.isEmpty()) {
            throw new OperacionInvalidaException("Revisa los campos marcados", "DATOS_INVALIDOS", campos);
        }
        repo.findFirstByCodigoIgnoreCase(Texto.limpio(dto.codigo()))
                .filter(otro -> !otro.getIdProducto().equals(idActual))
                .ifPresent(otro -> {
                    throw new ConflictoException(
                            "Ese código ya es de “" + otro.getNombreProducto() + "”.",
                            "PRODUCTO_DUPLICADO",
                            Map.of("codigo", "Ya hay un producto con este código."),
                            Map.of("idProductoExistente", otro.getIdProducto()));
                });
    }

    private static void convertirAEntity(ProductoRequest dto, ProductoEntity p) {
        p.setCodigo(Texto.limpio(dto.codigo()));
        p.setNombreProducto(Texto.limpio(dto.nombreProducto()));
        p.setDescripcion(Texto.limpio(dto.descripcion()));
        p.setIdMarca(dto.idMarca());
        p.setIdCategoria(dto.idCategoria());
        p.setCodUnidadMedida(dto.codUnidadMedida());
        p.setPrecioVenta(dto.precioVenta());
        p.setComisionFija(dto.comisionFija());
        p.setStockMinimo(dto.stockMinimo());
        p.setActivo(dto.activo());
    }

    private ProductoDetalleDTO convertirADetalle(ProductoEntity p) {
        int id = p.getIdProducto();
        Nombres nombres = consultas.nombres(id);
        return new ProductoDetalleDTO(
                id, p.getCodigo(), p.getNombreProducto(), p.getDescripcion(),
                p.getIdMarca(), p.getIdCategoria(), p.getCodUnidadMedida(),
                p.getPrecioVenta(), p.getComisionFija(), p.getStockMinimo(), p.isActivo(),
                nombres.nombreMarca(), nombres.nombreCategoria(),
                inventario.existencia(id),
                consultas.costoPeps(id).orElse(null),
                inventario.ultimoCosto(id).orElse(null),
                consultas.vendidos30Dias(id),
                consultas.lotesConExistencia(id),
                consultas.ultimosMovimientos(id, ULTIMOS_MOVIMIENTOS));
    }

    private static Map<String, Object> datosBitacora(ProductoEntity p) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("codigo", p.getCodigo());
        datos.put("nombreProducto", p.getNombreProducto());
        datos.put("descripcion", p.getDescripcion());
        datos.put("idMarca", p.getIdMarca());
        datos.put("idCategoria", p.getIdCategoria());
        datos.put("codUnidadMedida", p.getCodUnidadMedida());
        datos.put("precioVenta", p.getPrecioVenta());
        datos.put("comisionFija", p.getComisionFija());
        datos.put("stockMinimo", p.getStockMinimo());
        datos.put("activo", p.isActivo());
        return datos;
    }

    private static String conSigno(int n) {
        return n > 0 ? "+" + n : "−" + Math.abs(n);
    }

    private static String textoTipo(String tipo) {
        return switch (tipo) {
            case "DANADO" -> "dañado o vencido";
            case "USO_SALON" -> "uso en el salón";
            default -> "conteo físico";
        };
    }

    private static OperacionInvalidaException errorDeCampo(String campo, String mensaje) {
        return new OperacionInvalidaException(mensaje, "DATOS_INVALIDOS", Map.of(campo, mensaje));
    }
}