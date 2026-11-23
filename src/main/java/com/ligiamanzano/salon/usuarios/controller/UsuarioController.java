package com.ligiamanzano.salon.usuarios.controller;

import com.ligiamanzano.salon.common.request.ActivoRequest;
import com.ligiamanzano.salon.security.Permisos;
import com.ligiamanzano.salon.security.UsuarioAutenticado;
import com.ligiamanzano.salon.usuarios.dto.ContrasenaTemporalDTO;
import com.ligiamanzano.salon.usuarios.dto.UsuarioCreadoDTO;
import com.ligiamanzano.salon.usuarios.dto.UsuarioDTO;
import com.ligiamanzano.salon.usuarios.dto.UsuarioEdicionRequest;
import com.ligiamanzano.salon.usuarios.dto.UsuarioNuevoRequest;
import com.ligiamanzano.salon.usuarios.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@PreAuthorize(Permisos.ADMIN)
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService service;

    @GetMapping
    public List<UsuarioDTO> listar() {
        return service.listar();
    }

    @PostMapping
    public ResponseEntity<UsuarioCreadoDTO> crear(@Valid @RequestBody UsuarioNuevoRequest dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(dto));
    }

    @PutMapping("/{id}")
    public UsuarioDTO actualizar(@PathVariable int id, @Valid @RequestBody UsuarioEdicionRequest dto,
                                 @AuthenticationPrincipal UsuarioAutenticado actual) {
        return service.actualizar(id, dto, actual);
    }

    @PostMapping("/{id}/restablecer-contrasena")
    public ContrasenaTemporalDTO restablecerContrasena(@PathVariable int id) {
        return new ContrasenaTemporalDTO(service.restablecerContrasena(id));
    }

    @PatchMapping("/{id}/activo")
    public UsuarioDTO cambiarActivo(@PathVariable int id, @Valid @RequestBody ActivoRequest dto,
                                    @AuthenticationPrincipal UsuarioAutenticado actual) {
        return service.cambiarActivo(id, dto.activo(), actual);
    }
}
