package com.ligiamanzano.salon.usuarios.service;

import com.ligiamanzano.salon.auth.service.PoliticaContrasena;
import com.ligiamanzano.salon.bitacora.service.BitacoraService;
import com.ligiamanzano.salon.common.exception.OperacionInvalidaException;
import com.ligiamanzano.salon.common.exception.RecursoNoEncontradoException;
import com.ligiamanzano.salon.security.Rol;
import com.ligiamanzano.salon.security.UsuarioAutenticado;
import com.ligiamanzano.salon.usuarios.entity.UsuarioEntity;
import com.ligiamanzano.salon.usuarios.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    private static final String ENTIDAD = "tb_usuarios";

    private final UsuarioRepository repo;
    private final SesionService sesionService;
    private final PasswordEncoder encoder;
    private final PoliticaContrasena politica;
    private final BitacoraService bitacora;

    public UsuarioService(UsuarioRepository repo, SesionService sesionService, PasswordEncoder encoder,
                          PoliticaContrasena politica, BitacoraService bitacora) {
        this.repo = repo;
        this.sesionService = sesionService;
        this.encoder = encoder;
        this.politica = politica;
        this.bitacora = bitacora;
    }

    @Transactional
    public void cambiarActivo(int idUsuario, boolean activo, UsuarioAutenticado actual) {
        UsuarioEntity u = buscar(idUsuario);
        if (u.isActivo() == activo) {
            return;
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
        registrar(actual, activo ? "ACTIVAR_USUARIO" : "DESACTIVAR_USUARIO", u,
                (activo ? "Activó al usuario " : "Desactivó al usuario ") + u.getNombreUsuario());
    }

    @Transactional
    public String restablecerContrasena(int idUsuario, UsuarioAutenticado actual) {
        UsuarioEntity u = buscar(idUsuario);
        String temporal = politica.generarTemporal();
        u.setContrasenaHash(encoder.encode(temporal));
        u.setDebeCambiarContrasena(true);
        u.setIntentosFallidos((short) 0);
        u.setBloqueadoHasta(null);
        sesionService.revocarTodas(idUsuario, "ADMIN");
        registrar(actual, "RESTABLECER_CONTRASENA", u, "Restableció la contraseña de " + u.getNombreUsuario());
        return temporal;
    }

    @Transactional
    public void cambiarRol(int idUsuario, Rol nuevo, UsuarioAutenticado actual) {
        UsuarioEntity u = buscar(idUsuario);
        if (u.getIdRol() == nuevo.getId()) {
            return;
        }
        if (idUsuario == actual.idUsuario() && actual.rol() == Rol.ADMINISTRADOR) {
            throw new OperacionInvalidaException("No puedes quitarte el rol de Administrador.");
        }
        exigirOtroAdministradorActivo(u);
        Rol anterior = Rol.desdeId(u.getIdRol());
        u.setIdRol(nuevo.getId());
        sesionService.revocarTodas(idUsuario, "ADMIN");
        registrar(actual, "CAMBIO_ROL", u,
                "Cambió el rol de " + u.getNombreUsuario() + " de " + anterior.name() + " a " + nuevo.name());
    }

    private void exigirOtroAdministradorActivo(UsuarioEntity u) {
        boolean esAdministradorActivo = u.isActivo() && u.getIdRol() == Rol.ADMINISTRADOR.getId();
        if (esAdministradorActivo && repo.countByIdRolAndActivoTrue(Rol.ADMINISTRADOR.getId()) <= 1) {
            throw new OperacionInvalidaException("El sistema tiene que quedar con al menos un administrador activo.");
        }
    }

    private void registrar(UsuarioAutenticado actual, String accion, UsuarioEntity u, String descripcion) {
        bitacora.registrar(actual.idUsuario(), BitacoraService.SEGURIDAD, accion,
                ENTIDAD, String.valueOf(u.getIdUsuario()), descripcion);
    }

    private UsuarioEntity buscar(int idUsuario) {
        return repo.findById(idUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario no existe."));
    }
}
