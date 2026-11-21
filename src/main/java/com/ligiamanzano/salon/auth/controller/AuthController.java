package com.ligiamanzano.salon.auth.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ligiamanzano.salon.auth.dto.CambioContrasenaRequest;
import com.ligiamanzano.salon.auth.dto.LoginRequest;
import com.ligiamanzano.salon.auth.dto.LoginResponse;
import com.ligiamanzano.salon.auth.dto.UsuarioSesionDTO;
import com.ligiamanzano.salon.auth.service.AuthService;
import com.ligiamanzano.salon.security.UsuarioAutenticado;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req, HttpServletRequest http) {
        return authService.login(req, http.getRemoteAddr(), http.getHeader("User-Agent"));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        authService.logout(usuario);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public UsuarioSesionDTO me(@AuthenticationPrincipal UsuarioAutenticado u) {
        return new UsuarioSesionDTO(u.idUsuario(), u.nombreUsuario(), u.nombreCompleto(),
                u.rol().name(), u.debeCambiarContrasena());
    }

    @PostMapping("/cambiar-contrasena")
    public ResponseEntity<Void> cambiarContrasena(@AuthenticationPrincipal UsuarioAutenticado u,
                                                  @Valid @RequestBody CambioContrasenaRequest req) {
        authService.cambiarContrasena(u, req);
        return ResponseEntity.noContent().build();
    }
}
