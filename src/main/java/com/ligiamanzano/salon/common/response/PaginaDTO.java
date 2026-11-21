package com.ligiamanzano.salon.common.response;

import java.util.List;

public record PaginaDTO<T>(List<T> contenido, long totalElementos) {

    public static final int TAMANO_MAXIMO = 100;

    public static int tamanoSeguro(int tamano) {
        return Math.max(1, Math.min(tamano, TAMANO_MAXIMO));
    }
}
