package com.ligiamanzano.salon.servicios.service;

import com.ligiamanzano.salon.bitacora.service.BitacoraService;
import com.ligiamanzano.salon.common.exception.ConflictoException;
import com.ligiamanzano.salon.common.exception.OperacionInvalidaException;
import com.ligiamanzano.salon.common.exception.RecursoNoEncontradoException;
import com.ligiamanzano.salon.common.util.Fechas;
import com.ligiamanzano.salon.common.util.Texto;
import com.ligiamanzano.salon.configuracion.service.ParametroService;
import com.ligiamanzano.salon.personal.service.EspecialidadService;
import com.ligiamanzano.salon.servicios.dto.ServicioDTO;
import com.ligiamanzano.salon.servicios.dto.ServicioDetalleDTO;
import com.ligiamanzano.salon.servicios.dto.ServicioRequest;
import com.ligiamanzano.salon.servicios.dto.ServiciosCatalogoDTO;
import com.ligiamanzano.salon.servicios.entity.ServicioEntity;
import com.ligiamanzano.salon.servicios.repository.ServicioConsultaRepository;
import com.ligiamanzano.salon.servicios.repository.ServicioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ServicioService {

    private static final String ENTIDAD = "SERVICIO";

    private final ServicioRepository repo;
    private final ServicioConsultaRepository consultas;
    private final EspecialidadService especialidades;
    private final ParametroService parametros;
    private final BitacoraService bitacora;

    public ServiciosCatalogoDTO listar() {
        return new ServiciosCatalogoDTO(
                parametros.numero(ParametroService.COMISION_SERVICIO_PCT),
                parametros.numero(ParametroService.IVA_PCT),
                repo.findAllByOrderByNombreServicioAsc().stream()
                        .map(ServicioService::convertirADTO)
                        .toList());
    }

    public ServicioDetalleDTO obtener(int idServicio) {
        return convertirADetalle(buscar(idServicio));
    }

    @Transactional
    public ServicioDetalleDTO crear(ServicioRequest dto) {
        validar(dto, null);
        ServicioEntity s = new ServicioEntity();
        convertirAEntity(dto, s);
        s.setActivo(true);
        repo.save(s);
        bitacora.registrarCambios(BitacoraService.SERVICIOS, "CREAR", ENTIDAD, s.getIdServicio(),
                "Creó el servicio " + s.getNombreServicio(), null, datosBitacora(s));
        return convertirADetalle(s);
    }

    @Transactional
    public ServicioDetalleDTO actualizar(int idServicio, ServicioRequest dto) {
        ServicioEntity s = buscar(idServicio);
        validar(dto, idServicio);
        Map<String, Object> antes = datosBitacora(s);
        convertirAEntity(dto, s);
        bitacora.registrarCambios(BitacoraService.SERVICIOS, "EDITAR", ENTIDAD, idServicio,
                "Editó el servicio " + s.getNombreServicio(), antes, datosBitacora(s));
        return convertirADetalle(s);
    }

    @Transactional
    public ServicioDetalleDTO cambiarActivo(int idServicio, boolean activo) {
        ServicioEntity s = buscar(idServicio);
        if (s.isActivo() != activo) {
            s.setActivo(activo);
            bitacora.registrarCambios(BitacoraService.SERVICIOS, activo ? "ACTIVAR" : "DESACTIVAR", ENTIDAD, idServicio,
                    (activo ? "Activó el servicio " : "Desactivó el servicio ") + s.getNombreServicio(),
                    Map.of("activo", !activo), Map.of("activo", activo));
        }
        return convertirADetalle(s);
    }

    private ServicioEntity buscar(int idServicio) {
        return repo.findById(idServicio)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el servicio."));
    }

    private void validar(ServicioRequest dto, Integer idActual) {
        if (!especialidades.existe(dto.idEspecialidad())) {
            throw new OperacionInvalidaException("Revisa los campos marcados", "DATOS_INVALIDOS",
                    Map.of("idEspecialidad", "Esa especialidad no existe."));
        }
        repo.findFirstByNombreServicioIgnoreCase(Texto.limpio(dto.nombreServicio()))
                .filter(otro -> !otro.getIdServicio().equals(idActual))
                .ifPresent(otro -> {
                    throw new ConflictoException(
                            "Ya existe el servicio " + otro.getNombreServicio() + (otro.isActivo() ? "." : " (inactivo)."),
                            "SERVICIO_DUPLICADO",
                            Map.of("nombreServicio", "Ya hay un servicio con este nombre."),
                            Map.of("idServicioExistente", otro.getIdServicio()));
                });
    }

    private static void convertirAEntity(ServicioRequest dto, ServicioEntity s) {
        s.setNombreServicio(Texto.limpio(dto.nombreServicio()));
        s.setIdEspecialidad(dto.idEspecialidad());
        s.setDescripcion(Texto.limpio(dto.descripcion()));
        s.setDuracionMin(dto.duracionMin());
        s.setPrecio(dto.precio());
        s.setPrecioDesde(dto.precioDesde());
    }

    private static ServicioDTO convertirADTO(ServicioEntity s) {
        return new ServicioDTO(s.getIdServicio(), s.getNombreServicio(), s.getDescripcion(), s.getIdEspecialidad(),
                s.getDuracionMin(), s.getPrecio(), s.isPrecioDesde(), s.isActivo());
    }

    private ServicioDetalleDTO convertirADetalle(ServicioEntity s) {
        LocalDate primeroDelMes = Fechas.hoy().withDayOfMonth(1);
        OffsetDateTime desde = Fechas.inicioDelDia(primeroDelMes);
        OffsetDateTime hasta = Fechas.inicioDelDia(primeroDelMes.plusMonths(1));
        int id = s.getIdServicio();
        return new ServicioDetalleDTO(id, s.getNombreServicio(), s.getDescripcion(), s.getIdEspecialidad(),
                s.getDuracionMin(), s.getPrecio(), s.isPrecioDesde(), s.isActivo(),
                consultas.vecesEnElPeriodo(id, desde, hasta),
                consultas.tieneVentas(id),
                consultas.quienes(id, s.getIdEspecialidad(), desde, hasta));
    }

    private static Map<String, Object> datosBitacora(ServicioEntity s) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("nombreServicio", s.getNombreServicio());
        datos.put("idEspecialidad", s.getIdEspecialidad());
        datos.put("descripcion", s.getDescripcion());
        datos.put("duracionMin", s.getDuracionMin());
        datos.put("precio", s.getPrecio());
        datos.put("precioDesde", s.isPrecioDesde());
        return datos;
    }
}
