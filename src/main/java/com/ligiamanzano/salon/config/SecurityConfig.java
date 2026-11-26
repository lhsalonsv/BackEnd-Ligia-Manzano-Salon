package com.ligiamanzano.salon.config;

import com.ligiamanzano.salon.security.RespuestaError;
import com.ligiamanzano.salon.security.filter.JwtAuthFilter;
import com.ligiamanzano.salon.security.jwt.JwtService;
import com.ligiamanzano.salon.usuarios.service.SesionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] TODOS = {"ADMINISTRADOR", "RECEPCIONISTA", "CONTADOR", "AUDITOR"};
    private static final String[] PUEDEN_MODIFICAR = {"ADMINISTRADOR", "RECEPCIONISTA", "CONTADOR"};

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtService jwtService,
                                           SesionService sesionService) throws Exception {
        http
                .cors(Customizer.withDefaults())
                // Sin cookies de sesión el navegador no manda credenciales solo: no hay CSRF que proteger
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                        .requestMatchers("/api/auth/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/**").hasAnyRole(TODOS)
                        .requestMatchers("/api/**").hasAnyRole(PUEDEN_MODIFICAR)
                        .anyRequest().denyAll())

                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) ->
                                RespuestaError.escribir(res, 401, "NO_AUTENTICADO", "La sesión expiró. Inicia sesión de nuevo."))
                        .accessDeniedHandler((req, res, ex) -> {
                            if (tieneCambioPendiente()) {
                                RespuestaError.escribir(res, 403, "CAMBIO_CONTRASENA_REQUERIDO", "Debes cambiar tu contraseña para continuar.");
                            } else {
                                RespuestaError.escribir(res, 403, "SIN_PERMISO", "No tienes permiso para esta acción.");
                            }
                        }))

                .headers(h -> h
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
                        .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                        .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000)))

                .addFilterBefore(new JwtAuthFilter(jwtService, sesionService),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private static boolean tieneCambioPendiente() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> JwtAuthFilter.CAMBIO_PENDIENTE.equals(a.getAuthority()));
    }
}
