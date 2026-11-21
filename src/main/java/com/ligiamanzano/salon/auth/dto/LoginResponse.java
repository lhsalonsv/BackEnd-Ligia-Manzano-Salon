package com.ligiamanzano.salon.auth.dto;

public record LoginResponse(String token, UsuarioSesionDTO usuario) {}
