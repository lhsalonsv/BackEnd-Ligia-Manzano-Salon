package com.ligiamanzano.salon.usuarios.repository;

import com.ligiamanzano.salon.usuarios.entity.SesionEntity;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SesionRepository extends JpaRepository<SesionEntity, UUID> {
    @Query("select s from SesionEntity s join fetch s.usuario where s.idSesion = :id")
    Optional<SesionEntity> buscarConUsuario(@Param("id") UUID id);

    @Modifying
    @Query(value = """
        update tb_sesiones set revocada_en = now(), motivo_revocacion = :motivo
        where id_usuario = :idUsuario and revocada_en is null
    """, nativeQuery = true)
    int revocarTodas(@Param("idUsuario") int idUsuario, @Param("motivo") String motivo);

    @Modifying
    @Query(value = """
        update tb_sesiones set revocada_en = now(), motivo_revocacion = :motivo
        where id_usuario = :idUsuario and revocada_en is null and id_sesion <> :excepto
    """, nativeQuery = true)
    int revocarTodasMenos(@Param("idUsuario") int idUsuario, @Param("excepto") UUID excepto, @Param("motivo") String motivo);
}
