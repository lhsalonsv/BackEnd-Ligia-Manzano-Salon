package com.ligiamanzano.salon.bitacora.service;

import com.ligiamanzano.salon.bitacora.dto.BitacoraDetalleDTO;
import com.ligiamanzano.salon.bitacora.dto.BitacoraPaginaDTO;
import com.ligiamanzano.salon.bitacora.dto.BitacoraRegistroDTO;
import com.ligiamanzano.salon.bitacora.entity.BitacoraEntity;
import com.ligiamanzano.salon.bitacora.repository.BitacoraConsultaRepository;
import com.ligiamanzano.salon.bitacora.repository.BitacoraConsultaRepository.Filtro;
import com.ligiamanzano.salon.bitacora.repository.BitacoraRepository;
import com.ligiamanzano.salon.common.exception.OperacionInvalidaException;
import com.ligiamanzano.salon.common.exception.RecursoNoEncontradoException;
import com.ligiamanzano.salon.common.response.PaginaDTO;
import com.ligiamanzano.salon.common.util.Fechas;
import com.ligiamanzano.salon.common.util.Texto;
import com.ligiamanzano.salon.security.UsuarioAutenticado;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class BitacoraService {

    public static final String SESION = "SESION";
    public static final String SEGURIDAD = "SEGURIDAD";
    public static final String USUARIOS = "USUARIOS";
    public static final String CLIENTES = "CLIENTES";
    public static final String EMPLEADOS = "EMPLEADOS";
    public static final String SERVICIOS = "SERVICIOS";

    public static final String TODO = "todo";
    public static final String SISTEMA = "sistema";

    private static final int LARGO_DESCRIPCION = 300;
    private static final int LARGO_ENTIDAD_ID = 40;
    private static final int LARGO_EQUIPO = 120;

    private final BitacoraRepository repo;
    private final BitacoraConsultaRepository consultas;
    private final JsonMapper json;

    public BitacoraService(BitacoraRepository repo, BitacoraConsultaRepository consultas, JsonMapper json) {
        this.repo = repo;
        this.consultas = consultas;
        this.json = json;
    }

    @Transactional
    public void registrar(String modulo, String accion, String entidad, Object entidadId, String descripcion) {
        guardar(usuarioActual(), modulo, accion, entidad, entidadId, descripcion, null, null);
    }

    @Transactional
    public void registrarCambios(String modulo, String accion, String entidad, Object entidadId, String descripcion,
                                 Map<String, ?> antes, Map<String, ?> despues) {
        Map<String, Object> datosAntes = antes == null ? null : new LinkedHashMap<>(antes);
        Map<String, Object> datosDespues = despues == null ? null : new LinkedHashMap<>(despues);
        if (datosAntes != null && datosDespues != null) {
            quitarIguales(datosAntes, datosDespues);
            if (datosAntes.isEmpty() && datosDespues.isEmpty()) return;
        } else if (datosDespues != null) {
            datosDespues.values().removeIf(Objects::isNull);
        }
        guardar(usuarioActual(), modulo, accion, entidad, entidadId, descripcion, datosAntes, datosDespues);
    }

    @Transactional
    public void registrarSinSesion(Integer idUsuario, String modulo, String accion,
                                   String entidad, Object entidadId, String descripcion) {
        guardar(idUsuario, modulo, accion, entidad, entidadId, descripcion, null, null);
    }

    @Transactional(readOnly = true)
    public BitacoraPaginaDTO listar(LocalDate fecha, String modulo, String idUsuario, String buscar,
                                    int pagina, int tamano) {
        Filtro filtro = new Filtro(
                Fechas.inicioDelDia(fecha),
                Fechas.inicioDelDia(fecha.plusDays(1)),
                SISTEMA.equalsIgnoreCase(Texto.limpio(idUsuario)),
                persona(idUsuario),
                Texto.limpio(buscar));
        String moduloElegido = Texto.limpio(modulo) == null ? null : modulo.trim().toUpperCase(Locale.ROOT);

        Map<String, Long> conteos = consultas.conteos(filtro);
        long total = moduloElegido == null ? conteos.get(TODO) : conteos.getOrDefault(moduloElegido, 0L);
        List<BitacoraRegistroDTO> contenido = consultas.pagina(
                filtro, moduloElegido, Math.max(pagina, 0), PaginaDTO.tamanoSeguro(tamano));
        return new BitacoraPaginaDTO(contenido, total, conteos, consultas.personas(filtro.desde(), filtro.hasta()));
    }

    @Transactional(readOnly = true)
    public BitacoraDetalleDTO obtener(long idBitacora) {
        return consultas.detalle(idBitacora)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el registro."));
    }

    private void guardar(Integer idUsuario, String modulo, String accion, String entidad, Object entidadId,
                         String descripcion, Map<String, Object> antes, Map<String, Object> despues) {
        BitacoraEntity b = new BitacoraEntity();
        b.setIdUsuario(idUsuario);
        b.setModulo(modulo);
        b.setAccion(accion);
        b.setEntidad(entidad);
        b.setEntidadId(entidadId == null ? null : recortar(String.valueOf(entidadId), LARGO_ENTIDAD_ID));
        b.setDescripcion(recortar(descripcion, LARGO_DESCRIPCION));
        b.setDatosAntes(aJson(antes));
        b.setDatosDespues(aJson(despues));
        b.setEquipo(equipoActual());
        repo.save(b);
    }

    private String aJson(Map<String, Object> datos) {
        return datos == null || datos.isEmpty() ? null : json.writeValueAsString(datos);
    }

    private static void quitarIguales(Map<String, Object> antes, Map<String, Object> despues) {
        Set<String> claves = new HashSet<>(antes.keySet());
        claves.addAll(despues.keySet());
        for (String clave : claves) {
            if (iguales(antes.get(clave), despues.get(clave))) {
                antes.remove(clave);
                despues.remove(clave);
            }
        }
    }

    private static boolean iguales(Object a, Object b) {
        if (a instanceof BigDecimal x && b instanceof BigDecimal y) return x.compareTo(y) == 0;
        return Objects.equals(a, b);
    }

    private static Integer persona(String idUsuario) {
        String valor = Texto.limpio(idUsuario);
        if (valor == null || SISTEMA.equalsIgnoreCase(valor)) return null;
        try {
            return Integer.valueOf(valor);
        } catch (NumberFormatException e) {
            throw new OperacionInvalidaException("Revisa los campos marcados", "DATOS_INVALIDOS",
                    Map.of("idUsuario", "Elige una persona de la lista."));
        }
    }

    private static Integer usuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof UsuarioAutenticado u ? u.idUsuario() : null;
    }

    private static String equipoActual() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos)) {
            return null;
        }
        HttpServletRequest request = atributos.getRequest();
        return recortar(Equipo.desde(request.getHeader("User-Agent")), LARGO_EQUIPO);
    }

    private static String recortar(String valor, int max) {
        if (valor == null) return null;
        return valor.length() <= max ? valor : valor.substring(0, max);
    }
}
