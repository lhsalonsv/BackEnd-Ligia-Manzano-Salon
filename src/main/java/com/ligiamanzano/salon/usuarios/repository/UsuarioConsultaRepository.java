package com.ligiamanzano.salon.usuarios.repository;

import com.ligiamanzano.salon.common.util.Fechas;
import com.ligiamanzano.salon.security.Rol;
import com.ligiamanzano.salon.usuarios.dto.UsuarioDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UsuarioConsultaRepository {

    private static final String SELECT = """
            select u.id_usuario, u.nombre_usuario, u.nombre_completo, u.id_rol, u.activo,
                   u.debe_cambiar_contrasena, u.ultimo_acceso, e.id_empleado
              from tb_usuarios u
              left join tb_empleados e on e.id_usuario = u.id_usuario
            """;

    private final JdbcClient jdbc;

    public List<UsuarioDTO> lista() {
        return jdbc.sql(SELECT + " order by u.activo desc, u.nombre_completo")
                .query((rs, n) -> usuario(rs))
                .list();
    }

    public Optional<UsuarioDTO> porId(int idUsuario) {
        return jdbc.sql(SELECT + " where u.id_usuario = :idUsuario")
                .param("idUsuario", idUsuario)
                .query((rs, n) -> usuario(rs))
                .optional();
    }

    private static UsuarioDTO usuario(ResultSet rs) throws SQLException {
        OffsetDateTime ultimoAcceso = rs.getObject("ultimo_acceso", OffsetDateTime.class);
        return new UsuarioDTO(
                rs.getInt("id_usuario"),
                rs.getString("nombre_usuario"),
                rs.getString("nombre_completo"),
                Rol.desdeId(rs.getShort("id_rol")).name(),
                rs.getBoolean("activo"),
                rs.getBoolean("debe_cambiar_contrasena"),
                ultimoAcceso == null ? null : Fechas.local(ultimoAcceso).truncatedTo(ChronoUnit.SECONDS),
                rs.getObject("id_empleado", Integer.class));
    }
}
