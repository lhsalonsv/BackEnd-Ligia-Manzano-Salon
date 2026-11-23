package com.ligiamanzano.salon.clientes.repository;

import com.ligiamanzano.salon.clientes.dto.ClienteListaDTO;
import com.ligiamanzano.salon.clientes.dto.FormulaResumenDTO;
import com.ligiamanzano.salon.clientes.dto.ProximaCitaDTO;
import com.ligiamanzano.salon.clientes.dto.VisitaDTO;
import com.ligiamanzano.salon.common.util.Fechas;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ClienteConsultaRepository {

    //El texto que llega del frontend solo elige una opción del enum; nunca se pega en el SQL.
    public enum Filtro {
        TODOS("todos", "true"),
        ESTE_MES("este-mes", "date_trunc('month', s.ultima_visita) = date_trunc('month', current_date)"),
        SIN_VENIR_60("sin-venir-60", "s.ultima_visita < current_date - 60"),
        CUMPLE_MES("cumple-mes", "c.mes_cumple = extract(month from current_date)");

        private final String clave;
        private final String condicion;

        Filtro(String clave, String condicion) {
            this.clave = clave;
            this.condicion = condicion;
        }

        public String clave() {
            return clave;
        }

        public static Filtro desde(String clave) {
            for (Filtro f : values()) {
                if (f.clave.equals(clave)) return f;
            }
            return TODOS;
        }
    }

    public record Estadisticas(LocalDate ultimaVisita, int visitas12m, BigDecimal gastado12m) {}

    private static final String ESTADISTICAS = """
            with stats as (
                select v.id_cliente,
                       (max(v.fecha_hora) at time zone 'America/El_Salvador')::date as ultima_visita,
                       count(*) as visitas_12m,
                       sum(v.total) as gastado_12m
                  from tb_ventas v
                 where v.estado = 'COBRADA'
                   and v.fecha_hora >= now() - interval '12 months'
                 group by v.id_cliente
            )
            """;

    private static final String BUSQUEDA = """
              from tb_clientes c
              left join stats s on s.id_cliente = c.id_cliente
             where c.activo
               and (cast(:texto as text) is null
                    or unaccent(lower(c.nombre || ' ' || coalesce(c.apellido, '')))
                       like '%' || unaccent(lower(cast(:texto as text))) || '%'
                    or (length(cast(:digitos as text)) >= 3
                        and (regexp_replace(c.telefono, '\\D', '', 'g') like '%' || cast(:digitos as text) || '%'
                             or regexp_replace(coalesce(c.num_documento, ''), '\\D', '', 'g')
                                like '%' || cast(:digitos as text) || '%')))
            """;

    private static final String PROXIMA_CITA = """
            select cs.inicio, sv.nombre_servicio as servicio, split_part(e.nombre_empleado, ' ', 1) as empleado
              from tb_citas ci
              join tb_cita_servicios cs on cs.id_cita = ci.id_cita
              join tb_servicios sv on sv.id_servicio = cs.id_servicio
              left join tb_empleados e on e.id_empleado = cs.id_empleado
             where ci.id_cliente = %s
               and ci.estado not in ('CANCELADA', 'NO_VINO')
               and cs.inicio >= now()
             order by cs.inicio
             limit 1
            """;

    private final JdbcClient jdbc;

    public Map<String, Long> conteos(String texto, String digitos) {
        StringBuilder sql = new StringBuilder(ESTADISTICAS).append("select count(*) as \"todos\"");
        for (Filtro f : Filtro.values()) {
            if (f != Filtro.TODOS) {
                sql.append(", count(*) filter (where ").append(f.condicion).append(") as \"").append(f.clave).append('"');
            }
        }
        sql.append(BUSQUEDA);
        return jdbc.sql(sql.toString())
                .param("texto", texto)
                .param("digitos", digitos)
                .query((rs, n) -> {
                    Map<String, Long> conteos = new LinkedHashMap<>();
                    for (Filtro f : Filtro.values()) {
                        conteos.put(f.clave, rs.getLong(f.clave));
                    }
                    return conteos;
                })
                .single();
    }

    public List<ClienteListaDTO> pagina(String texto, String digitos, Filtro filtro, int pagina, int tamano) {
        String sql = ESTADISTICAS + """
                , pagina as (
                    select c.id_cliente, c.nombre, c.apellido, c.telefono,
                           coalesce(c.alergias, '') <> '' as tiene_alergia,
                           s.ultima_visita,
                           coalesce(s.visitas_12m, 0) as visitas_12m,
                           coalesce(s.gastado_12m, 0.00) as gastado_12m
                """ + BUSQUEDA + "   and " + filtro.condicion + """

                     order by s.ultima_visita desc nulls last, c.nombre, c.id_cliente
                     limit :tamano offset :desde
                )
                select p.*, pc.inicio as proxima_inicio, pc.servicio as proxima_servicio, pc.empleado as proxima_empleado
                  from pagina p
                  left join lateral (
                """ + PROXIMA_CITA.formatted("p.id_cliente") + """
                  ) pc on true
                 order by p.ultima_visita desc nulls last, p.nombre, p.id_cliente
                """;
        return jdbc.sql(sql)
                .param("texto", texto)
                .param("digitos", digitos)
                .param("tamano", tamano)
                .param("desde", pagina * tamano)
                .query((rs, n) -> new ClienteListaDTO(
                        rs.getInt("id_cliente"),
                        rs.getString("nombre"),
                        rs.getString("apellido"),
                        rs.getString("telefono"),
                        rs.getBoolean("tiene_alergia"),
                        rs.getObject("ultima_visita", LocalDate.class),
                        rs.getInt("visitas_12m"),
                        rs.getBigDecimal("gastado_12m"),
                        proximaCita(rs, "proxima_")))
                .list();
    }

    public Estadisticas estadisticas(int idCliente) {
        return jdbc.sql("""
                        select (max(v.fecha_hora) at time zone 'America/El_Salvador')::date as ultima_visita,
                               count(*) as visitas_12m,
                               coalesce(sum(v.total), 0.00) as gastado_12m
                          from tb_ventas v
                         where v.id_cliente = :idCliente
                           and v.estado = 'COBRADA'
                           and v.fecha_hora >= now() - interval '12 months'
                        """)
                .param("idCliente", idCliente)
                .query((rs, n) -> new Estadisticas(
                        rs.getObject("ultima_visita", LocalDate.class),
                        rs.getInt("visitas_12m"),
                        rs.getBigDecimal("gastado_12m")))
                .single();
    }

    public String empleadoFrecuente(int idCliente) {
        return jdbc.sql("""
                        select e.nombre_empleado || ' ' || e.apellido
                          from tb_detalle_ventas d
                          join tb_ventas v on v.id_venta = d.id_venta
                          join tb_empleados e on e.id_empleado = d.id_empleado
                         where v.id_cliente = :idCliente
                           and v.estado = 'COBRADA'
                           and v.fecha_hora >= now() - interval '12 months'
                         group by e.id_empleado, e.nombre_empleado, e.apellido
                         order by count(distinct v.id_venta) desc, max(v.fecha_hora) desc
                         limit 1
                        """)
                .param("idCliente", idCliente)
                .query(String.class)
                .optional()
                .orElse(null);
    }

    public Optional<FormulaResumenDTO> ultimaFormula(int idCliente) {
        return jdbc.sql("""
                        select f.fecha, split_part(e.nombre_empleado, ' ', 1) as empleado, f.formula, f.observaciones
                          from tb_formulas_color f
                          join tb_empleados e on e.id_empleado = f.id_empleado
                         where f.id_cliente = :idCliente
                         order by f.fecha desc, f.id_formula desc
                         limit 1
                        """)
                .param("idCliente", idCliente)
                .query((rs, n) -> new FormulaResumenDTO(
                        rs.getObject("fecha", LocalDate.class),
                        rs.getString("empleado"),
                        rs.getString("formula"),
                        rs.getString("observaciones")))
                .optional();
    }

    public List<VisitaDTO> ultimasVisitas(int idCliente) {
        return jdbc.sql("""
                        select (v.fecha_hora at time zone 'America/El_Salvador')::date as fecha,
                               v.total,
                               (select string_agg(d.descripcion, ' + ' order by d.id_detalle_venta)
                                  from tb_detalle_ventas d
                                 where d.id_venta = v.id_venta and d.id_servicio is not null) as servicios,
                               (select coalesce(sum(d.cantidad), 0)
                                  from tb_detalle_ventas d
                                 where d.id_venta = v.id_venta and d.id_producto is not null) as productos,
                               (select e.nombre_empleado || ' ' || e.apellido
                                  from tb_detalle_ventas d
                                  join tb_empleados e on e.id_empleado = d.id_empleado
                                 where d.id_venta = v.id_venta
                                 order by (d.id_servicio is null), d.id_detalle_venta
                                 limit 1) as empleado
                          from tb_ventas v
                         where v.id_cliente = :idCliente
                           and v.estado = 'COBRADA'
                         order by v.fecha_hora desc
                         limit 3
                        """)
                .param("idCliente", idCliente)
                .query((rs, n) -> new VisitaDTO(
                        rs.getObject("fecha", LocalDate.class),
                        describir(rs.getString("servicios"), rs.getInt("productos")),
                        rs.getString("empleado"),
                        rs.getBigDecimal("total")))
                .list();
    }

    //Busca los datos en la base
    public Optional<ProximaCitaDTO> proximaCita(int idCliente) {
        return jdbc.sql(PROXIMA_CITA.formatted(":idCliente"))
                .param("idCliente", idCliente)
                .query((rs, n) -> proximaCita(rs, ""))
                .optional();
    }

    //Convierte los datos traídos a un DTO
    private static ProximaCitaDTO proximaCita(ResultSet rs, String prefijo) throws SQLException {
        OffsetDateTime inicio = rs.getObject(prefijo + "inicio", OffsetDateTime.class);
        if (inicio == null) return null;
        return new ProximaCitaDTO(Fechas.local(inicio), rs.getString(prefijo + "servicio"), rs.getString(prefijo + "empleado"));
    }

    //arma el texto corto que se muestra en las últimas visitas de la ficha del cliente
    private static String describir(String servicios, int productos) {
        String textoProductos = productos == 0 ? null : productos + (productos == 1 ? " producto" : " productos");
        if (servicios == null) return textoProductos == null ? "Venta" : textoProductos;
        return textoProductos == null ? servicios : servicios + " + " + textoProductos;
    }
}
