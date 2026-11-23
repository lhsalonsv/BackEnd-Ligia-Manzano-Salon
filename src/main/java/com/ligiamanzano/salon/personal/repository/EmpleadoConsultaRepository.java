package com.ligiamanzano.salon.personal.repository;

import com.ligiamanzano.salon.common.util.Fechas;
import com.ligiamanzano.salon.personal.dto.EmpleadoListaDTO;
import com.ligiamanzano.salon.personal.dto.EmpleadoResumenDTO;
import com.ligiamanzano.salon.personal.dto.EspecialidadDTO;
import com.ligiamanzano.salon.personal.dto.UsuarioLigadoDTO;
import com.ligiamanzano.salon.security.Rol;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class EmpleadoConsultaRepository {

    private final JdbcClient jdbc;

    /**
     * Une empleados con su usuario (para el rol) y suma las comisiones de la quincena.
     * Las comisiones por devolución ya vienen en negativo, así que la suma las resta sola.
     */
    public List<EmpleadoListaDTO> lista(LocalDate inicioQuincena, LocalDate finQuincena) {
        return jdbc.sql("""
                        select e.id_empleado, e.nombre_empleado, e.apellido, e.puesto, e.dui, u.id_rol,
                               e.sueldo_base_mensual, e.recibe_comision, e.activo, e.fecha_baja,
                               coalesce((select sum(co.monto)
                                           from tb_comisiones co
                                          where co.id_empleado = e.id_empleado
                                            and co.fecha >= :desde
                                            and co.fecha < :hasta), 0.00) as comisiones_quincena
                          from tb_empleados e
                          left join tb_usuarios u on u.id_usuario = e.id_usuario
                         order by e.activo desc, e.nombre_empleado, e.apellido
                        """)
                .param("desde", Fechas.inicioDelDia(inicioQuincena))
                .param("hasta", Fechas.inicioDelDia(finQuincena.plusDays(1)))
                .query((rs, n) -> new EmpleadoListaDTO(
                        rs.getInt("id_empleado"),
                        rs.getString("nombre_empleado"),
                        rs.getString("apellido"),
                        rs.getString("puesto"),
                        rs.getString("dui"),
                        rol(rs),
                        rs.getBigDecimal("sueldo_base_mensual"),
                        rs.getBoolean("recibe_comision"),
                        rs.getBigDecimal("comisiones_quincena"),
                        rs.getBoolean("activo"),
                        rs.getObject("fecha_baja", LocalDate.class)))
                .list();
    }

    /**
     * El usuario y rol de la ficha (null si no entra al sistema)
     * */
    public Optional<UsuarioLigadoDTO> usuarioLigado(int idUsuario) {
        return jdbc.sql("select nombre_usuario, id_rol from tb_usuarios where id_usuario = :idUsuario")
                .param("idUsuario", idUsuario)
                .query((rs, n) -> new UsuarioLigadoDTO(rs.getString("nombre_usuario"), rol(rs)))
                .optional();
    }

    public List<EspecialidadDTO> especialidadesConEmpleados() {
        Map<Integer, EspecialidadDTO> porId = new LinkedHashMap<>();
        jdbc.sql("""
                        select es.id_especialidad, es.nombre_especialidad, e.id_empleado,
                               e.nombre_empleado || ' ' || e.apellido as nombre_completo, e.puesto
                          from tb_especialidades es
                          left join (tb_empleado_especialidad ee
                                     join tb_empleados e on e.id_empleado = ee.id_empleado and e.activo)
                                 on ee.id_especialidad = es.id_especialidad
                         order by es.nombre_especialidad, e.nombre_empleado, e.apellido
                        """)
                .query((RowCallbackHandler) rs -> {
                    int idEspecialidad = rs.getInt("id_especialidad");
                    String nombre = rs.getString("nombre_especialidad");
                    EspecialidadDTO especialidad = porId.computeIfAbsent(
                            idEspecialidad, id -> new EspecialidadDTO(id, nombre, new ArrayList<>()));
                    int idEmpleado = rs.getInt("id_empleado");
                    if (!rs.wasNull()) {
                        especialidad.empleados().add(new EmpleadoResumenDTO(
                                idEmpleado, rs.getString("nombre_completo"), rs.getString("puesto")));
                    }
                });
        return new ArrayList<>(porId.values());
    }

    private static String rol(ResultSet rs) throws SQLException {
        short idRol = rs.getShort("id_rol");
        return rs.wasNull() ? null : Rol.desdeId(idRol).name();
    }
}