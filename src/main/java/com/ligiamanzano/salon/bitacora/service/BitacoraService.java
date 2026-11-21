package com.ligiamanzano.salon.bitacora.service;

import com.ligiamanzano.salon.bitacora.entity.BitacoraEntity;
import com.ligiamanzano.salon.bitacora.repository.BitacoraRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class BitacoraService {

    public static final String SEGURIDAD = "SEGURIDAD";

    private static final int LARGO_DESCRIPCION = 300;
    private static final int LARGO_EQUIPO = 120;

    private final BitacoraRepository repo;

    public BitacoraService(BitacoraRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public void registrar(Integer idUsuario, String modulo, String accion, String descripcion) {
        registrar(idUsuario, modulo, accion, null, null, descripcion);
    }

    @Transactional
    public void registrar(Integer idUsuario, String modulo, String accion,
                          String entidad, String entidadId, String descripcion) {
        BitacoraEntity b = new BitacoraEntity();
        b.setIdUsuario(idUsuario);
        b.setModulo(modulo);
        b.setAccion(accion);
        b.setEntidad(entidad);
        b.setEntidadId(entidadId);
        b.setDescripcion(recortar(descripcion, LARGO_DESCRIPCION));
        b.setEquipo(equipoActual());
        repo.save(b);
    }

    private static String equipoActual() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos)) {
            return null;
        }
        HttpServletRequest request = atributos.getRequest();
        String navegador = request.getHeader("User-Agent");
        String equipo = request.getRemoteAddr() + (navegador == null ? "" : " · " + navegador);
        return recortar(equipo, LARGO_EQUIPO);
    }

    private static String recortar(String valor, int max) {
        if (valor == null) return null;
        return valor.length() <= max ? valor : valor.substring(0, max);
    }
}
