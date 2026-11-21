package com.ligiamanzano.salon.usuarios.repository;

import com.ligiamanzano.salon.usuarios.entity.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<UsuarioEntity, Integer> {
    Optional<UsuarioEntity> findByNombreUsuario(String nombreUsuario);

    long countByIdRolAndActivoTrue(Short idRol);
}
