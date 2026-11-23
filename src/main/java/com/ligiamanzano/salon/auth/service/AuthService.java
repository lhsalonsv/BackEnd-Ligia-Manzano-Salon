package com.ligiamanzano.salon.auth.service;

import com.ligiamanzano.salon.auth.dto.CambioContrasenaRequest;
import com.ligiamanzano.salon.auth.dto.LoginRequest;
import com.ligiamanzano.salon.auth.dto.LoginResponse;
import com.ligiamanzano.salon.auth.dto.UsuarioSesionDTO;
import com.ligiamanzano.salon.bitacora.service.BitacoraService;
import com.ligiamanzano.salon.common.exception.CredencialesInvalidasException;
import com.ligiamanzano.salon.common.exception.OperacionInvalidaException;
import com.ligiamanzano.salon.config.SeguridadProperties;
import com.ligiamanzano.salon.security.Rol;
import com.ligiamanzano.salon.security.UsuarioAutenticado;
import com.ligiamanzano.salon.security.jwt.JwtService;
import com.ligiamanzano.salon.usuarios.entity.SesionEntity;
import com.ligiamanzano.salon.usuarios.entity.UsuarioEntity;
import com.ligiamanzano.salon.usuarios.repository.UsuarioRepository;
import com.ligiamanzano.salon.usuarios.service.SesionService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;

@Service
public class AuthService {

    private static final String USUARIO = "USUARIO";

    private final UsuarioRepository usuarioRepo;
    private final SesionService sesionService;
    private final JwtService jwtService;
    private final PasswordEncoder encoder;
    private final PoliticaContrasena politica;
    private final SeguridadProperties props;
    private final BitacoraService bitacora;
    private final String hashFalso;

    public AuthService(UsuarioRepository usuarioRepo, SesionService sesionService,
                       JwtService jwtService, PasswordEncoder encoder, PoliticaContrasena politica,
                       SeguridadProperties props, BitacoraService bitacora) {
        this.usuarioRepo = usuarioRepo;
        this.sesionService = sesionService;
        this.jwtService = jwtService;
        this.encoder = encoder;
        this.politica = politica;
        this.props = props;
        this.bitacora = bitacora;
        this.hashFalso = encoder.encode("hash-para-igualar-tiempos");
    }

    @Transactional(noRollbackFor = CredencialesInvalidasException.class)
    public LoginResponse login(LoginRequest req, String ip, String userAgent) {
        String nombre = req.nombreUsuario().trim().toLowerCase(Locale.ROOT);
        UsuarioEntity u = usuarioRepo.findByNombreUsuario(nombre).orElse(null);
        Instant ahora = Instant.now();

        if (u == null) {
            encoder.matches(req.contrasena(), hashFalso);
            bitacora.registrarSinSesion(null, BitacoraService.SEGURIDAD, "USUARIO_INEXISTENTE", null, null,
                    "Intentó entrar con un usuario que no existe: " + recortar(nombre, 50));
            throw new CredencialesInvalidasException();
        }

        if (u.getBloqueadoHasta() != null && u.getBloqueadoHasta().isAfter(ahora)) {
            bitacora.registrarSinSesion(null, BitacoraService.SEGURIDAD, "ENTRADA_BLOQUEADA", USUARIO, u.getIdUsuario(),
                    "Intentó entrar a " + u.getNombreUsuario() + " con la cuenta bloqueada");
            throw new CredencialesInvalidasException();
        }

        if (!encoder.matches(req.contrasena(), u.getContrasenaHash())) {
            short intentos = (short) (u.getIntentosFallidos() + 1);
            if (intentos >= props.intentosMaximos()) {
                u.setBloqueadoHasta(ahora.plus(props.bloqueo()));
                u.setIntentosFallidos((short) 0);
                bitacora.registrarSinSesion(null, BitacoraService.SEGURIDAD, "CUENTA_BLOQUEADA", USUARIO, u.getIdUsuario(),
                        "Bloqueó " + u.getNombreUsuario() + " " + props.bloqueo().toMinutes() + " minutos por "
                                + props.intentosMaximos() + " contraseñas incorrectas");
            } else {
                u.setIntentosFallidos(intentos);
                bitacora.registrarSinSesion(null, BitacoraService.SEGURIDAD, "CONTRASENA_INCORRECTA", USUARIO, u.getIdUsuario(),
                        "Contraseña incorrecta para " + u.getNombreUsuario() + " · " + intentos
                                + (intentos == 1 ? " intento" : " intentos"));
            }
            throw new CredencialesInvalidasException();
        }

        if (!u.isActivo()) {
            bitacora.registrarSinSesion(null, BitacoraService.SEGURIDAD, "USUARIO_INACTIVO", USUARIO, u.getIdUsuario(),
                    "Intentó entrar con el usuario desactivado " + u.getNombreUsuario());
            throw new CredencialesInvalidasException();
        }

        u.setIntentosFallidos((short) 0);
        u.setBloqueadoHasta(null);
        u.setUltimoAcceso(ahora);

        SesionEntity sesion = sesionService.crear(u, ip, userAgent);
        String token = jwtService.generar(u.getIdUsuario(), sesion.getIdSesion(), sesion.getExpiraEn());
        bitacora.registrarSinSesion(u.getIdUsuario(), BitacoraService.SESION, "ENTRAR", USUARIO, u.getIdUsuario(),
                "Inició sesión");

        return new LoginResponse(token, new UsuarioSesionDTO(
                u.getIdUsuario(), u.getNombreUsuario(), u.getNombreCompleto(),
                Rol.desdeId(u.getIdRol()).name(), u.isDebeCambiarContrasena()));
    }

    @Transactional
    public void logout(UsuarioAutenticado actual) {
        sesionService.revocar(actual.idSesion(), "LOGOUT");
        bitacora.registrar(BitacoraService.SESION, "SALIR", USUARIO, actual.idUsuario(), "Cerró sesión");
    }

    @Transactional
    public void cambiarContrasena(UsuarioAutenticado actual, CambioContrasenaRequest req) {
        UsuarioEntity u = usuarioRepo.findById(actual.idUsuario()).orElseThrow();

        if (!encoder.matches(req.contrasenaActual(), u.getContrasenaHash())) {
            // 400 y no 401: un 401 haría que el frontend cierre la sesión
            throw new OperacionInvalidaException("Revisa los datos marcados.", "CONTRASENA_ACTUAL_INCORRECTA",
                    Map.of("contrasenaActual", "La contraseña actual no es correcta."));
        }
        politica.validar(req.contrasenaNueva(), u.getNombreUsuario());
        if (encoder.matches(req.contrasenaNueva(), u.getContrasenaHash())) {
            throw new OperacionInvalidaException("Revisa los datos marcados.", "CONTRASENA_REPETIDA",
                    Map.of("contrasenaNueva", "Tiene que ser distinta de la actual."));
        }

        u.setContrasenaHash(encoder.encode(req.contrasenaNueva()));
        u.setDebeCambiarContrasena(false);
        sesionService.revocarTodas(u.getIdUsuario(), "CAMBIO_CONTRASENA");
        bitacora.registrar(BitacoraService.SESION, "CAMBIAR_CONTRASENA", USUARIO, u.getIdUsuario(),
                "Cambió su contraseña");
    }

    private static String recortar(String valor, int max) {
        return valor.length() <= max ? valor : valor.substring(0, max);
    }
}
