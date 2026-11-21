package com.ligiamanzano.salon.security;

public enum Rol {
    ADMINISTRADOR(1), RECEPCIONISTA(2), CONTADOR(3), AUDITOR(4);

    private final short id;

    Rol(int id) { this.id = (short) id; }

    public short getId() { return id; }

    public static Rol desdeId(short id) {
        for (Rol r : values()) {
            if (r.id == id) return r;
        }
        throw new IllegalStateException("Rol desconocido: " + id);
    }
}
