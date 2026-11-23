package com.ligiamanzano.salon.bitacora.service;

final class Equipo {

    static final String DESCONOCIDO = "Equipo desconocido";

    private Equipo() {}

    static String desde(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) return DESCONOCIDO;
        String navegador = navegador(userAgent);
        String sistema = sistema(userAgent);
        if (navegador == null && sistema == null) return DESCONOCIDO;
        if (navegador == null) return "Navegador en " + sistema;
        if (sistema == null) return navegador;
        return navegador + " en " + sistema;
    }

    private static String navegador(String ua) {
        if (ua.contains("Edg/") || ua.contains("EdgA/") || ua.contains("EdgiOS/")) return "Edge";
        if (ua.contains("OPR/") || ua.contains("Opera")) return "Opera";
        if (ua.contains("SamsungBrowser/")) return "Samsung Internet";
        if (ua.contains("Firefox/") || ua.contains("FxiOS/")) return "Firefox";
        if (ua.contains("Chrome/") || ua.contains("CriOS/")) return "Chrome";
        if (ua.contains("Safari/") && ua.contains("Version/")) return "Safari";
        if (ua.contains("PowerShell")) return "PowerShell";
        if (ua.contains("PostmanRuntime")) return "Postman";
        if (ua.startsWith("curl/")) return "curl";
        return null;
    }

    private static String sistema(String ua) {
        if (ua.contains("Android")) return "Android";
        if (ua.contains("iPhone")) return "iPhone";
        if (ua.contains("iPad")) return "iPad";
        if (ua.contains("Windows")) return "Windows";
        if (ua.contains("CrOS")) return "ChromeOS";
        if (ua.contains("Macintosh") || ua.contains("Mac OS X")) return "Mac";
        if (ua.contains("Linux")) return "Linux";
        return null;
    }
}
