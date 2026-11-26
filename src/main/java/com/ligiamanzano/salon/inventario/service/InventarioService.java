package com.ligiamanzano.salon.inventario.service;

import com.ligiamanzano.salon.inventario.entity.LoteEntity;
import com.ligiamanzano.salon.inventario.entity.MovimientoInventarioEntity;
import com.ligiamanzano.salon.inventario.entity.ProductoEntity;
import com.ligiamanzano.salon.inventario.repository.LoteRepository;
import com.ligiamanzano.salon.inventario.repository.MovimientoInventarioRepository;
import com.ligiamanzano.salon.inventario.repository.PepsRepository;
import com.ligiamanzano.salon.inventario.repository.ProductoRepository;
import com.ligiamanzano.salon.inventario.service.Peps.Salida;
import com.ligiamanzano.salon.notificaciones.service.NotificacionService;
import com.ligiamanzano.salon.security.UsuarioActual;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class InventarioService {

    public static final String ENTRADA_COMPRA = "ENTRADA_COMPRA";
    public static final String ENTRADA_AJUSTE = "ENTRADA_AJUSTE";
    public static final String SALIDA_VENTA = "SALIDA_VENTA";
    public static final String SALIDA_AJUSTE = "SALIDA_AJUSTE";
    public static final String USO_SALON = "USO_SALON";

    private final LoteRepository loteRepo;
    private final MovimientoInventarioRepository movimientoRepo;
    private final PepsRepository peps;
    private final ProductoRepository productoRepo;
    private final NotificacionService notificaciones;

    public int existencia(int idProducto) {
        return peps.existencia(idProducto);
    }

    public Optional<BigDecimal> ultimoCosto(int idProducto) {
        return peps.ultimoCosto(idProducto);
    }

    //Crea el lote (con cantidad_inicial; la base lo deja en 0) y su movimiento positivo; el trigger le sube la existencia.
    @Transactional
    public void entrada(int idProducto, String tipo, int cantidad, BigDecimal costoUnitario,
                        Integer idDetalleCompra, String motivo) {
        LoteEntity lote = new LoteEntity();
        lote.setIdProducto(idProducto);
        lote.setIdDetalleCompra(idDetalleCompra);
        lote.setCostoUnitario(costoUnitario);
        lote.setCantidadInicial(cantidad);
        loteRepo.save(lote);
        guardarMovimiento(lote.getIdLote(), idProducto, tipo, cantidad, costoUnitario, idDetalleCompra, null, motivo);
    }

    //Bloquea, reparte con Peps, inserta un movimiento negativo por cada lote tocado, avisa el stock bajo y devuelve el costo total a 2 decimales.
    @Transactional
    public BigDecimal consumir(int idProducto, int cantidad, String tipo, String motivo, Integer idDetalleVenta) {
        List<Peps.LoteDisponible> lotes = peps.bloquearLotesConExistencia(idProducto);
        int antes = lotes.stream().mapToInt(Peps.LoteDisponible::disponible).sum();
        List<Salida> salidas = Peps.repartir(lotes, cantidad);
        BigDecimal costo = BigDecimal.ZERO;
        for (Salida s : salidas) {
            guardarMovimiento(s.idLote(), idProducto, tipo, -s.cantidad(), s.costoUnitario(), null, idDetalleVenta, motivo);
            costo = costo.add(s.costo());
        }
        avisarSiQuedaBajo(idProducto, antes, antes - cantidad);
        return costo.setScale(2, RoundingMode.HALF_UP);
    }

    private void guardarMovimiento(int idLote, int idProducto, String tipo, int cantidad, BigDecimal costoUnitario,
                                   Integer idDetalleCompra, Integer idDetalleVenta, String motivo) {
        MovimientoInventarioEntity m = new MovimientoInventarioEntity();
        m.setIdLote(idLote);
        m.setIdProducto(idProducto);
        m.setTipo(tipo);
        m.setCantidad(cantidad);
        m.setCostoUnitario(costoUnitario);
        m.setIdDetalleCompra(idDetalleCompra);
        m.setIdDetalleVenta(idDetalleVenta);
        m.setMotivo(motivo);
        m.setIdUsuario(UsuarioActual.id());
        movimientoRepo.save(m);
    }

    private void avisarSiQuedaBajo(int idProducto, int antes, int despues) {
        ProductoEntity p = productoRepo.findById(idProducto).orElseThrow();
        int minimo = p.getStockMinimo();
        if (minimo > 0 && antes >= minimo && despues < minimo) {
            notificaciones.stockBajo(idProducto, p.getNombreProducto(), despues, minimo);
        }
    }
}
