package com.ligiamanzano.salon.common.exception;

public class CredencialesInvalidasException extends RuntimeException {
    public CredencialesInvalidasException() {
        super("Usuario o contraseña incorrectos. Después de 5 intentos seguidos la cuenta se bloquea 15 minutos.");
    }
}
