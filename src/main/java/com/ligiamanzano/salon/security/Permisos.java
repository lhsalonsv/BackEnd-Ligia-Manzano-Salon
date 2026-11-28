package com.ligiamanzano.salon.security;

public final class Permisos {

    private Permisos() {}

    public static final String ADMIN            = "hasRole('ADMINISTRADOR')";
    public static final String CAJA             = "hasAnyRole('ADMINISTRADOR','RECEPCIONISTA')";
    public static final String CONTABILIDAD     = "hasAnyRole('ADMINISTRADOR','CONTADOR','AUDITOR')";
    public static final String LECTURA_CONTABLE = "hasAnyRole('ADMINISTRADOR','CONTADOR','AUDITOR')";
    public static final String AUDITORIA        = "hasAnyRole('ADMINISTRADOR','AUDITOR')";
}
