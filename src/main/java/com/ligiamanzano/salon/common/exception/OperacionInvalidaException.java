package com.ligiamanzano.salon.common.exception;

import java.util.Map;

public class OperacionInvalidaException extends RuntimeException {

    private final String codigo;
    private final Map<String, String> campos;

    public OperacionInvalidaException(String mensaje) {
        this(mensaje, "OPERACION_INVALIDA", null);
    }

    public OperacionInvalidaException(String mensaje, String codigo, Map<String, String> campos) {
        super(mensaje);
        this.codigo = codigo;
        this.campos = campos;
    }

    public String getCodigo() { return codigo; }

    public Map<String, String> getCampos() { return campos; }
}
