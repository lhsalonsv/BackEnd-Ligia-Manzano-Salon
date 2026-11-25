package com.ligiamanzano.salon.notificaciones.service;

import com.ligiamanzano.salon.notificaciones.entity.NotificacionEntity;
import com.ligiamanzano.salon.notificaciones.repository.NotificacionRepository;
import com.ligiamanzano.salon.security.Rol;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificacionService {

    public static final String STOCK_BAJO = "STOCK_BAJO";

    private static final int LARGO_TITULO = 120;

    private final NotificacionRepository repo;

    @Transactional
    public void stockBajo(int idProducto, String nombreProducto, int existencia, int minimo) {
        NotificacionEntity n = new NotificacionEntity();
        n.setTipo(STOCK_BAJO);
        n.setTitulo(recortar("Stock bajo: " + nombreProducto));
        n.setMensaje(existencia == 0
                ? "Se agotó. El mínimo es " + minimo + "."
                : "Quedan " + existencia + " y el mínimo es " + minimo + ".");
        n.setEntidad("PRODUCTO");
        n.setEntidadId(String.valueOf(idProducto));
        n.setIdRolDestino(Rol.ADMINISTRADOR.getId());
        repo.save(n);
    }

    private static String recortar(String texto) {
        return texto.length() <= LARGO_TITULO ? texto : texto.substring(0, LARGO_TITULO);
    }
}