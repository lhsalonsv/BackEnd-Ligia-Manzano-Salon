package com.ligiamanzano.salon.security;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public final class RespuestaError {

    private RespuestaError() {}

    public static void escribir(HttpServletResponse res, int status, String codigo, String mensaje) throws IOException {
        res.setStatus(status);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write("{\"mensaje\":\"" + mensaje + "\",\"codigo\":\"" + codigo + "\"}");
    }
}
