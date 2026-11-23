package com.ligiamanzano.salon.servicios.repository;

import com.ligiamanzano.salon.servicios.dto.QuienRealizaDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ServicioConsultaRepository {

    private static final String LINEAS_COBRADAS = """
            select count(*)
              from tb_detalle_ventas d
              join tb_ventas v on v.id_venta = d.id_venta
             where d.id_servicio = :idServicio
               and v.estado = 'COBRADA'
               and v.fecha_hora >= :desde and v.fecha_hora < :hasta
            """;

    private final JdbcClient jdbc;

    public int vecesEnElPeriodo(int idServicio, OffsetDateTime desde, OffsetDateTime hasta) {
        return jdbc.sql(LINEAS_COBRADAS)
                .param("idServicio", idServicio)
                .param("desde", desde)
                .param("hasta", hasta)
                .query(Integer.class)
                .single();
    }

    public boolean tieneVentas(int idServicio) {
        return jdbc.sql("select exists (select 1 from tb_detalle_ventas where id_servicio = :idServicio)")
                .param("idServicio", idServicio)
                .query(Boolean.class)
                .single();
    }

    public List<QuienRealizaDTO> quienes(int idServicio, int idEspecialidad, OffsetDateTime desde, OffsetDateTime hasta) {
        return jdbc.sql("""
                        select e.id_empleado, e.nombre_empleado || ' ' || e.apellido as nombre_completo, e.puesto,
                               (""" + LINEAS_COBRADAS + """
                                   and d.id_empleado = e.id_empleado) as veces
                          from tb_empleados e
                          join tb_empleado_especialidad ee on ee.id_empleado = e.id_empleado
                         where ee.id_especialidad = :idEspecialidad
                           and e.activo
                         order by veces desc, e.nombre_empleado, e.apellido
                        """)
                .param("idServicio", idServicio)
                .param("idEspecialidad", idEspecialidad)
                .param("desde", desde)
                .param("hasta", hasta)
                .query((rs, n) -> new QuienRealizaDTO(
                        rs.getInt("id_empleado"),
                        rs.getString("nombre_completo"),
                        rs.getString("puesto"),
                        rs.getInt("veces")))
                .list();
    }
}
