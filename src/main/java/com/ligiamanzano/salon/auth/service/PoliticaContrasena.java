package com.ligiamanzano.salon.auth.service;

import com.ligiamanzano.salon.common.exception.OperacionInvalidaException;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class PoliticaContrasena {

    public static final int MINIMO = 12;
    private static final int MAXIMO = 64;

    private static final Set<String> PROHIBIDAS = Set.of(
            "123456789012", "contrasena123", "password1234", "qwertyuiopas",
            "ligiamanzano", "ligiamanzano1", "salonligia123", "administrador");

    private static final String ALFABETO_TEMPORAL = "abcdefghijkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int GRUPOS_TEMPORAL = 3;
    private static final int LARGO_GRUPO_TEMPORAL = 4;

    private final SecureRandom azar = new SecureRandom();

    public void validar(String nueva, String nombreUsuario) {
        String error = null;
        String n = nueva.toLowerCase(Locale.ROOT);
        if (nueva.length() < MINIMO) {
            error = "Tiene que tener al menos " + MINIMO + " caracteres.";
        } else if (nueva.length() > MAXIMO) {
            error = "Puede tener hasta " + MAXIMO + " caracteres.";
        } else if (n.contains(nombreUsuario.toLowerCase(Locale.ROOT))) {
            error = "No puede contener tu usuario.";
        } else if (PROHIBIDAS.contains(n)) {
            error = "Es demasiado común.";
        }
        if (error != null) {
            throw new OperacionInvalidaException("Revisa los datos marcados.", "CONTRASENA_DEBIL",
                    Map.of("contrasenaNueva", error));
        }
    }

    public String generarTemporal() {
        StringBuilder clave = new StringBuilder();
        for (int g = 0; g < GRUPOS_TEMPORAL; g++) {
            if (g > 0) clave.append('-');
            for (int i = 0; i < LARGO_GRUPO_TEMPORAL; i++) {
                clave.append(ALFABETO_TEMPORAL.charAt(azar.nextInt(ALFABETO_TEMPORAL.length())));
            }
        }
        return clave.toString();
    }
}
