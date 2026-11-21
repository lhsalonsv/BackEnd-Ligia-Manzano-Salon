package com.ligiamanzano.salon.usuarios.service;

import com.ligiamanzano.salon.config.SeguridadProperties;
import com.ligiamanzano.salon.security.Rol;
import com.ligiamanzano.salon.security.UsuarioAutenticado;
import com.ligiamanzano.salon.usuarios.entity.SesionEntity;
import com.ligiamanzano.salon.usuarios.entity.UsuarioEntity;
import com.ligiamanzano.salon.usuarios.repository.SesionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class SesionService {

    private static final Duration ACTUALIZAR_ACTIVIDAD_CADA = Duration.ofMinutes(1);

    private final SesionRepository repo;
    private final SeguridadProperties props;

    public SesionService(SesionRepository repo, SeguridadProperties props) {
        this.repo = repo;
        this.props = props;
    }

    @Transactional
    public SesionEntity crear(UsuarioEntity usuario, String ip, String userAgent) {
        Instant ahora = Instant.now();
        SesionEntity s = new SesionEntity();
        s.setIdSesion(UUID.randomUUID());
        s.setUsuario(usuario);
        s.setCreadaEn(ahora);
        s.setUltimaActividad(ahora);
        s.setExpiraEn(ahora.plus(props.sesionAbsoluta()));
        s.setIp(recortar(ip, 45));
        s.setUserAgent(recortar(userAgent, 300));
        return repo.save(s);
    }

    @Transactional
    public Optional<UsuarioAutenticado> validar(UUID idSesion, int idUsuario) {
        SesionEntity s = repo.buscarConUsuario(idSesion).orElse(null);
        if (s == null || s.getRevocadaEn() != null || !s.getUsuario().getIdUsuario().equals(idUsuario)) {
            return Optional.empty();
        }

        Instant ahora = Instant.now();
        if (ahora.isAfter(s.getExpiraEn())) {
            revocar(s, "EXPIRADA");
            return Optional.empty();
        }

        if (ahora.isAfter(s.getUltimaActividad().plus(props.inactividad()))) {
            revocar(s, "INACTIVIDAD");
            return Optional.empty();
        }

        UsuarioEntity u = s.getUsuario();
        if (!u.isActivo()) {
            revocar(s, "DESACTIVADO");
            return Optional.empty();
        }

        if (s.getUltimaActividad().plus(ACTUALIZAR_ACTIVIDAD_CADA).isBefore(ahora)) {
            s.setUltimaActividad(ahora);
        }

        return Optional.of(new UsuarioAutenticado(
                u.getIdUsuario(), u.getNombreUsuario(), u.getNombreCompleto(),
                Rol.desdeId(u.getIdRol()), u.isDebeCambiarContrasena(), s.getIdSesion()));
    }

    @Transactional
    public void revocar(UUID idSesion, String motivo) {
        repo.findById(idSesion).ifPresent(s -> {
            if (s.getRevocadaEn() == null) revocar(s, motivo);
        });
    }

    @Transactional
    public int revocarTodas(int idUsuario, String motivo) {
        return repo.revocarTodas(idUsuario, motivo);
    }

    private void revocar(SesionEntity s, String motivo) {
        s.setRevocadaEn(Instant.now());
        s.setMotivoRevocacion(motivo);
    }

    private static String recortar(String valor, int max) {
        if (valor == null) return null;
        return valor.length() <= max ? valor : valor.substring(0, max);
    }
}