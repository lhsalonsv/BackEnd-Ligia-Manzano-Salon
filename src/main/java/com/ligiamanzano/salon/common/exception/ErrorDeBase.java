package com.ligiamanzano.salon.common.exception;

import java.sql.SQLException;
import java.util.Optional;

public final class ErrorDeBase {

    private static final String REGLA_DE_LA_BASE = "P0001";

    private ErrorDeBase() {}

    public static Optional<String> mensaje(Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (t instanceof SQLException sql && REGLA_DE_LA_BASE.equals(sql.getSQLState())) {
                return Optional.of(limpiar(sql.getMessage()));
            }
        }
        return Optional.empty();
    }

    private static String limpiar(String texto) {
        String mensaje = texto == null ? "" : texto;
        int salto = mensaje.indexOf('\n');
        if (salto >= 0) mensaje = mensaje.substring(0, salto);
        return mensaje.startsWith("ERROR: ") ? mensaje.substring("ERROR: ".length()) : mensaje;
    }
}
