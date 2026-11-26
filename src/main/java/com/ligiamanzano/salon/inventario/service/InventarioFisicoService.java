package com.ligiamanzano.salon.inventario.service;

import com.ligiamanzano.salon.bitacora.service.BitacoraService;
import com.ligiamanzano.salon.common.exception.ConflictoException;
import com.ligiamanzano.salon.common.exception.OperacionInvalidaException;
import com.ligiamanzano.salon.common.exception.RecursoNoEncontradoException;
import com.ligiamanzano.salon.common.util.Fechas;
import com.ligiamanzano.salon.common.util.Texto;
import com.ligiamanzano.salon.configuracion.service.EmisorService;
import com.ligiamanzano.salon.inventario.dto.*;
import com.ligiamanzano.salon.inventario.entity.ConteoFisicoEntity;
import com.ligiamanzano.salon.inventario.entity.InventarioFisicoEntity;
import com.ligiamanzano.salon.inventario.entity.ProductoEntity;
import com.ligiamanzano.salon.inventario.repository.ConteoFisicoRepository;
import com.ligiamanzano.salon.inventario.repository.InventarioFisicoConsultaRepository;
import com.ligiamanzano.salon.inventario.repository.InventarioFisicoRepository;
import com.ligiamanzano.salon.inventario.repository.ProductoRepository;
import com.ligiamanzano.salon.security.UsuarioActual;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class InventarioFisicoService {

    private static final String ENTIDAD = "INVENTARIO_FISICO";
    private static final String BORRADOR = "BORRADOR";
    private static final String CERRADO = "CERRADO";
    private static final int LARGO_NOTA = 150;
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final InventarioFisicoRepository repo;
    private final ConteoFisicoRepository conteoRepo;
    private final InventarioFisicoConsultaRepository consultas;
    private final ProductoRepository productos;
    private final EmisorService emisor;
    private final BitacoraService bitacora;
    private final EntityManager entityManager;

    public List<InventarioFisicoListaDTO> listar() {
        return consultas.lista();
    }

    public InventarioFisicoDTO obtener(int id) {
        return convertirADTO(buscar(id));
    }

    @Transactional
    public InventarioFisicoDTO crear(InventarioFisicoRequest dto) {
        if (dto.fechaCorte().isAfter(Fechas.hoy())) {
            throw errorDeCampo("fechaCorte", "La fecha de corte no puede ser futura.");
        }
        if (repo.existsByFechaCorte(dto.fechaCorte())) {
            throw new ConflictoException("Ya hay un inventario físico con esa fecha de corte.", "INVENTARIO_FISICO_DUPLICADO",
                    Map.of("fechaCorte", "Ya hay un conteo con esta fecha."));
        }
        if (repo.findFirstByEstado(BORRADOR).isPresent()) {
            throw errorDeCampo("fechaCorte", "Ya hay un conteo abierto: ciérralo antes de empezar otro.");
        }
        InventarioFisicoEntity f = new InventarioFisicoEntity();
        f.setFechaCorte(dto.fechaCorte());
        f.setEstado(BORRADOR);
        f.setIdUsuario(UsuarioActual.id());
        repo.saveAndFlush(f);
        bitacora.registrar(BitacoraService.INVENTARIO, "CREAR", ENTIDAD, f.getIdInventarioFisico(),
                "Abrió el inventario físico al " + DIA.format(f.getFechaCorte()));
        return convertirADTO(f);
    }

    @Transactional
    public InventarioFisicoDTO guardarConteo(int id, List<ConteoRequest> filas) {
        InventarioFisicoEntity f = buscar(id);
        if (CERRADO.equals(f.getEstado())) {
            throw new ConflictoException("El inventario ya está cerrado y no se puede cambiar.", "INVENTARIO_CERRADO", null);
        }
        String problema = revisarFilas(filas);
        if (problema != null) throw errorDeCampo("conteo", problema);

        for (ConteoRequest fila : filas) {
            ConteoFisicoEntity c = conteoRepo.findFirstByIdInventarioFisicoAndIdProducto(id, fila.idProducto())
                    .orElseGet(ConteoFisicoEntity::new);
            c.setIdInventarioFisico(id);
            c.setIdProducto(fila.idProducto());
            c.setCantidadContada(fila.cantidadContada());
            c.setNota(Texto.limpio(fila.nota()));
            conteoRepo.save(c);
        }
        conteoRepo.flush();
        bitacora.registrar(BitacoraService.INVENTARIO, "EDITAR", ENTIDAD, id,
                "Guardó el conteo de " + productosTexto(filas.size()) + " del inventario físico al " + DIA.format(f.getFechaCorte()));
        return convertirADTO(f);
    }

    @Transactional
    public InventarioFisicoDTO cerrar(int id, CierreRequest dto) {
        InventarioFisicoEntity f = repo.bloquear(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el inventario físico."));
        if (CERRADO.equals(f.getEstado())) {
            throw new ConflictoException("El inventario ya está cerrado.", "INVENTARIO_CERRADO", null);
        }
        List<ProductoPendienteDTO> faltan = consultas.sinContar(id);
        if (!faltan.isEmpty()) {
            throw new OperacionInvalidaException("Faltan " + productosTexto(faltan.size()) + " por contar.",
                    "DATOS_INVALIDOS", Map.of("conteo", faltan));
        }

        int ajustes = consultas.cerrar(id, UsuarioActual.id(), dto == null ? null : Texto.limpio(dto.observaciones()));
        entityManager.refresh(f);

        bitacora.registrarCambios(BitacoraService.INVENTARIO, "CERRAR", ENTIDAD, id,
                "Cerró el inventario físico al " + DIA.format(f.getFechaCorte()) + ": "
                        + (ajustes == 0 ? "sin diferencias" : productosTexto(ajustes) + " con diferencia"),
                Map.of("estado", BORRADOR), Map.of("estado", CERRADO, "diferencias", ajustes));
        return convertirADTO(f);
    }

    private InventarioFisicoEntity buscar(int id) {
        return repo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el inventario físico."));
    }

    private String revisarFilas(List<ConteoRequest> filas) {
        if (filas == null || filas.isEmpty()) return "Manda al menos un producto contado.";
        for (ConteoRequest fila : filas) {
            if (fila.cantidadContada() == null || fila.cantidadContada() < 0) {
                return "Hay cantidades inválidas: tienen que ser enteros desde 0.";
            }
            ProductoEntity p = fila.idProducto() == null ? null : productos.findById(fila.idProducto()).orElse(null);
            if (p == null) return "El producto " + fila.idProducto() + " no existe.";
            if (!p.isActivo()) return "El producto " + p.getNombreProducto() + " está inactivo.";
            String nota = Texto.limpio(fila.nota());
            if (nota != null && nota.length() > LARGO_NOTA) {
                return "La nota de " + p.getNombreProducto() + " admite hasta " + LARGO_NOTA + " caracteres.";
            }
        }
        return null;
    }

    private InventarioFisicoDTO convertirADTO(InventarioFisicoEntity f) {
        int id = f.getIdInventarioFisico();
        boolean cerrado = CERRADO.equals(f.getEstado());
        return new InventarioFisicoDTO(
                id,
                f.getFechaCorte(),
                f.getEstado(),
                cerrado ? Fechas.local(f.getCerradoEn()) : null,
                f.getObservaciones(),
                emisor.encabezado(),
                consultas.conteo(id, cerrado),
                cerrado ? consultas.acta(id) : null,
                cerrado ? consultas.ajustes(id) : null);
    }

    private static String productosTexto(int n) {
        return n == 1 ? "1 producto" : n + " productos";
    }

    private static OperacionInvalidaException errorDeCampo(String campo, String mensaje) {
        return new OperacionInvalidaException(mensaje, "DATOS_INVALIDOS", Map.of(campo, mensaje));
    }
}
