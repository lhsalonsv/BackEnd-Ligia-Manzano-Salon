package com.ligiamanzano.salon.config;

import com.ligiamanzano.salon.security.Rol;
import com.ligiamanzano.salon.usuarios.entity.UsuarioEntity;
import com.ligiamanzano.salon.usuarios.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Crea el administrador solo si la tabla está vacía y solo si existe la variable ADMIN_INICIAL_PASSWORD.
 * Entra con debe_cambiar_contrasena =true
 */
@Configuration
public class AdminInicial {

    @Bean
    CommandLineRunner crearAdminInicial(UsuarioRepository repo, PasswordEncoder encoder,
                                        @Value("${ADMIN_INICIAL_PASSWORD:}") String clave) {
        return args -> {
            if (clave.isBlank() || repo.count() > 0) return;
            UsuarioEntity u = new UsuarioEntity();
            u.setNombreUsuario("admin");
            u.setNombreCompleto("Administrador");
            u.setContrasenaHash(encoder.encode(clave));
            u.setIdRol(Rol.ADMINISTRADOR.getId());
            u.setDebeCambiarContrasena(true);
            u.setActivo(true);
            repo.save(u);
        };
    }
}
