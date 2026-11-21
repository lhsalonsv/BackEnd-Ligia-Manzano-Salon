package com.ligiamanzano.salon.security.filter;

import com.ligiamanzano.salon.security.UsuarioAutenticado;
import com.ligiamanzano.salon.security.jwt.JwtService;
import com.ligiamanzano.salon.usuarios.service.SesionService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class JwtAuthFilter extends OncePerRequestFilter {

    public static final String CAMBIO_PENDIENTE = "ROLE_CAMBIO_PENDIENTE";
    private static final String PREFIJO = "Bearer ";

    private final JwtService jwtService;
    private final SesionService sesionService;

    public JwtAuthFilter(JwtService jwtService, SesionService sesionService) {
        this.jwtService = jwtService;
        this.sesionService = sesionService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String cabecera = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (cabecera != null && cabecera.startsWith(PREFIJO)) {
            String token = cabecera.substring(PREFIJO.length()).trim();
            try {
                Claims claims = jwtService.validar(token);
                int idUsuario = Integer.parseInt(claims.getSubject());
                UUID idSesion = UUID.fromString(claims.getId());

                sesionService.validar(idSesion, idUsuario).ifPresent(usuario -> {
                    var auth = UsernamePasswordAuthenticationToken.authenticated(
                            usuario, null, autoridades(usuario));
                    SecurityContext ctx = SecurityContextHolder.createEmptyContext();
                    ctx.setAuthentication(auth);
                    SecurityContextHolder.setContext(ctx);
                });
            } catch (JwtException | IllegalArgumentException e) {
                // Firma alterada, token vencido o mal formado: sigue como anónimo y Security responde 401.
            }
        }
        chain.doFilter(request, response);
    }

    /** Con clave temporal no recibe su rol: solo puede usar /api/auth/**. */
    private List<GrantedAuthority> autoridades(UsuarioAutenticado u) {
        if (u.debeCambiarContrasena()) {
            return List.of(new SimpleGrantedAuthority(CAMBIO_PENDIENTE));
        }
        return List.of(new SimpleGrantedAuthority("ROLE_" + u.rol().name()));
    }
}
