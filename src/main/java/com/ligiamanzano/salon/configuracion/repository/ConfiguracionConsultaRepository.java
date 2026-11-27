package com.ligiamanzano.salon.configuracion.repository;

import com.ligiamanzano.salon.common.util.Fechas;
import com.ligiamanzano.salon.configuracion.dto.UltimoCambioDTO;
import com.ligiamanzano.salon.configuracion.dto.UltimoEnvioDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ConfiguracionConsultaRepository {

    public record Turno(String nombreEmpleada, short diaSemana, LocalTime horaInicio, LocalTime horaFin) {}

    private final JdbcClient jdbc;

    public UltimoCambioDTO ultimoCambio(String modulo, String entidad) {
        return jdbc.sql("""
                        select b.fecha_hora, u.nombre_completo
                          from tb_bitacora b
                          left join tb_usuarios u on u.id_usuario = b.id_usuario
                         where b.modulo = :modulo and b.entidad = :entidad
                         order by b.fecha_hora desc, b.id_bitacora desc
                         limit 1
                        """)
                .param("modulo", modulo)
                .param("entidad", entidad)
                .query((rs, n) -> new UltimoCambioDTO(fecha(rs, "fecha_hora"), rs.getString("nombre_completo")))
                .optional()
                .orElse(null);
    }

    public UltimoEnvioDTO ultimoEnvio() {
        return jdbc.sql("""
                        select t.enviado_en, t.estado, d.numero_control
                          from tb_dtes_transmisiones t
                          join tb_dtes d on d.id_dte = t.id_dte
                         order by t.enviado_en desc, t.id_dte_transmision desc
                         limit 1
                        """)
                .query((rs, n) -> new UltimoEnvioDTO(fecha(rs, "enviado_en"), rs.getString("estado"), rs.getString("numero_control")))
                .optional()
                .orElse(null);
    }

    public List<Turno> turnosDeEmpleadasActivas() {
        return jdbc.sql("""
                        select e.nombre_empleado || ' ' || e.apellido as nombre, h.dia_semana, h.hora_inicio, h.hora_fin
                          from tb_horarios_empleado h
                          join tb_empleados e on e.id_empleado = h.id_empleado
                         where e.activo and e.fecha_baja is null
                         order by e.nombre_empleado, e.apellido, h.dia_semana, h.hora_inicio
                        """)
                .query((rs, n) -> new Turno(
                        rs.getString("nombre"),
                        rs.getShort("dia_semana"),
                        rs.getObject("hora_inicio", LocalTime.class),
                        rs.getObject("hora_fin", LocalTime.class)))
                .list();
    }

    private static OffsetDateTime fecha(ResultSet rs, String columna) throws SQLException {
        OffsetDateTime valor = rs.getObject(columna, OffsetDateTime.class);
        return valor == null ? null : Fechas.local(valor).truncatedTo(ChronoUnit.SECONDS);
    }
}
