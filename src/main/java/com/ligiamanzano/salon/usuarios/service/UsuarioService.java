package com.ligiamanzano.salon.usuarios.service;

import com.ligiamanzano.salon.auth.service.PoliticaContrasena;
import com.ligiamanzano.salon.bitacora.service.BitacoraService;
import com.ligiamanzano.salon.common.exception.ConflictoException;
import com.ligiamanzano.salon.common.exception.OperacionInvalidaException;
import com.ligiamanzano.salon.common.exception.RecursoNoEncontradoException;
import com.ligiamanzano.salon.common.util.Texto;
import com.ligiamanzano.salon.personal.service.EmpleadoService;
import com.ligiamanzano.salon.security.Rol;
import com.ligiamanzano.salon.security.UsuarioAutenticado;
import com.ligiamanzano.salon.usuarios.dto.UsuarioCreadoDTO;
import com.ligiamanzano.salon.usuarios.dto.UsuarioDTO;
import com.ligiamanzano.salon.usuarios.dto.UsuarioEdicionRequest;
import com.ligiamanzano.salon.usuarios.dto.UsuarioNuevoRequest;
import com.ligiamanzano.salon.usuarios.entity.UsuarioEntity;
import com.ligiamanzano.salon.usuarios.repository.UsuarioConsultaRepository;
import com.ligiamanzano.salon.usuarios.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class UsuarioService {

    private static final String ENTIDAD = "USUARIO";

    private final UsuarioRepository repo;
    private final UsuarioConsultaRepository consultas;
    private final SesionService sesionService;
    private final EmpleadoService empleados;
    private final PasswordEncoder encoder;
    private final PoliticaContrasena politica;
    private final BitacoraService bitacora;

    public UsuarioService(UsuarioRepository repo, UsuarioConsultaRepository consultas, SesionService sesionService,
                          EmpleadoService empleados, PasswordEncoder encoder, PoliticaContrasena politica,
                          BitacoraService bitacora) {
        this.repo = repo;
        this.consultas = consultas;
        this.sesionService = sesionService;
        this.empleados = empleados;
        this.encoder = encoder;
        this.politica = politica;
        this.bitacora = bitacora;
    }

    public List<UsuarioDTO> listar() {
        return consultas.lista();
    }

    @Transactional
    public UsuarioCreadoDTO crear(UsuarioNuevoRequest dto) {
        String nombreUsuario = dto.nombreUsuario().trim().toLowerCase(Locale.ROOT);
        repo.findByNombreUsuario(nombreUsuario).ifPresent(otro -> {
            throw new ConflictoException("Ese nombre de usuario ya existe.", "USUARIO_DUPLICADO",
                    Map.of("nombreUsuario", "Ya hay un usuario con este nombre."));
        });
        Rol rol = Rol.valueOf(dto.rol());
        String temporal = politica.generarTemporal();

        UsuarioEntity u = new UsuarioEntity();
        u.setNombreUsuario(nombreUsuario);
        u.setNombreCompleto(Texto.limpio(dto.nombreCompleto()));
        u.setContrasenaHash(encoder.encode(temporal));
        u.setIdRol(rol.getId());
        u.setDebeCambiarContrasena(true);
        u.setActivo(true);
        repo.save(u);
        empleados.ligarUsuario(dto.idEmpleado(), u.getIdUsuario());

        bitacora.registrarCambios(BitacoraService.USUARIOS, "CREAR", ENTIDAD, u.getIdUsuario(),
                "Creó el usuario " + nombreUsuario + " (" + nombreRol(rol) + ") con contraseña temporal",
                null, datosBitacora(u, dto.idEmpleado()));
        return new UsuarioCreadoDTO(convertirADTO(u.getIdUsuario()), temporal);
    }

    @Transactional
    public UsuarioDTO actualizar(int idUsuario, UsuarioEdicionRequest dto, UsuarioAutenticado actual) {
        UsuarioEntity u = buscar(idUsuario);
        Integer empleadoAntes = convertirADTO(idUsuario).idEmpleado();
        Map<String, Object> antes = datosBitacora(u, empleadoAntes);
        Rol anterior = Rol.desdeId(u.getIdRol());
        Rol nuevo = Rol.valueOf(dto.rol());

        if (anterior != nuevo) {
            if (idUsuario == actual.idUsuario() && anterior == Rol.ADMINISTRADOR) {
                throw new OperacionInvalidaException("No puedes quitarte el rol de Administrador.");
            }
            exigirOtroAdministradorActivo(u);
            u.setIdRol(nuevo.getId());
            sesionService.revocarTodas(idUsuario, "ADMIN");
        }
        u.setNombreCompleto(Texto.limpio(dto.nombreCompleto()));
        empleados.ligarUsuario(dto.idEmpleado(), idUsuario);

        String descripcion = anterior != nuevo
                ? "Cambió el rol de " + u.getNombreUsuario() + " de " + nombreRol(anterior) + " a " + nombreRol(nuevo)
                : "Editó el usuario " + u.getNombreUsuario();
        bitacora.registrarCambios(BitacoraService.USUARIOS, "EDITAR", ENTIDAD, idUsuario, descripcion,
                antes, datosBitacora(u, dto.idEmpleado()));
        return convertirADTO(idUsuario);
    }

    @Transactional
    public UsuarioDTO cambiarActivo(int idUsuario, boolean activo, UsuarioAutenticado actual) {
        UsuarioEntity u = buscar(idUsuario);
        if (u.isActivo() == activo) {
            return convertirADTO(idUsuario);
        }
        if (!activo) {
            if (idUsuario == actual.idUsuario()) {
                throw new OperacionInvalidaException("No puedes desactivar tu propio usuario.");
            }
            exigirOtroAdministradorActivo(u);
        }
        u.setActivo(activo);
        if (!activo) {
            sesionService.revocarTodas(idUsuario, "DESACTIVADO");
        }
        bitacora.registrarCambios(BitacoraService.USUARIOS, activo ? "ACTIVAR" : "DESACTIVAR", ENTIDAD, idUsuario,
                (activo ? "Activó al usuario " : "Desactivó al usuario ") + u.getNombreUsuario(),
                Map.of("activo", !activo), Map.of("activo", activo));
        return convertirADTO(idUsuario);
    }

    @Transactional
    public String restablecerContrasena(int idUsuario) {
        UsuarioEntity u = buscar(idUsuario);
        String temporal = politica.generarTemporal();
        u.setContrasenaHash(encoder.encode(temporal));
        u.setDebeCambiarContrasena(true);
        u.setIntentosFallidos((short) 0);
        u.setBloqueadoHasta(null);
        sesionService.revocarTodas(idUsuario, "ADMIN");
        bitacora.registrar(BitacoraService.USUARIOS, "RESTABLECER_CONTRASENA", ENTIDAD, idUsuario,
                "Restableció la contraseña de " + u.getNombreUsuario());
        return temporal;
    }

    private void exigirOtroAdministradorActivo(UsuarioEntity u) {
        boolean esAdministradorActivo = u.isActivo() && u.getIdRol() == Rol.ADMINISTRADOR.getId();
        if (esAdministradorActivo && repo.countByIdRolAndActivoTrue(Rol.ADMINISTRADOR.getId()) <= 1) {
            throw new OperacionInvalidaException("El sistema tiene que quedar con al menos un administrador activo.");
        }
    }

    private UsuarioEntity buscar(int idUsuario) {
        return repo.findById(idUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario no existe."));
    }

    private UsuarioDTO convertirADTO(int idUsuario) {
        repo.flush();
        return consultas.porId(idUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario no existe."));
    }

    private static Map<String, Object> datosBitacora(UsuarioEntity u, Integer idEmpleado) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("nombreUsuario", u.getNombreUsuario());
        datos.put("nombreCompleto", u.getNombreCompleto());
        datos.put("rol", Rol.desdeId(u.getIdRol()).name());
        datos.put("idEmpleado", idEmpleado);
        return datos;
    }

    private static String nombreRol(Rol rol) {
        String nombre = rol.name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(nombre.charAt(0)) + nombre.substring(1);
    }
}
