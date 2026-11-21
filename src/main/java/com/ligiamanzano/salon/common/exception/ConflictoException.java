package com.ligiamanzano.salon.common.exception;

import java.util.Map;

public class ConflictoException extends RuntimeException {

    private final String codigo;
    private final Map<String, String> campos;
    private final Map<String, Object> extra;

    public ConflictoException(String mensaje) {
        this(mensaje, "CONFLICTO", null, null);
    }

    public ConflictoException(String mensaje, String codigo, Map<String, String> campos) {
        this(mensaje, codigo, campos, null);
    }

    public ConflictoException(String mensaje, String codigo, Map<String, String> campos, Map<String, Object> extra) {
        super(mensaje);
        this.codigo = codigo;
        this.campos = campos;
        this.extra = extra;
    }

    public String getCodigo() { return codigo; }

    public Map<String, String> getCampos() { return campos; }

    public Map<String, Object> getExtra() { return extra; }
}
