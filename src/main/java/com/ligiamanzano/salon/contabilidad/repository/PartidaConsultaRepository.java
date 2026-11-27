package com.ligiamanzano.salon.contabilidad.repository;

import com.ligiamanzano.salon.common.util.Fechas;
import com.ligiamanzano.salon.contabilidad.dto.LineaPartidaDTO;
import com.ligiamanzano.salon.contabilidad.dto.PartidaListaDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PartidaConsultaRepository {

    public record Filtro(Short anio, Short mes, String tipo, String estado, String buscar) {}

    private static final String COLUMNAS = """
            select p.id_partida, p.anio, p.mes, p.numero, p.fecha, p.codigo_tipo, p.concepto, p.estado, p.origen,
                   coalesce(t.total_debe, 0.00) as total_debe, coalesce(t.total_haber, 0.00) as total_haber,
                   p.id_partida_anula, pa.numero as numero_anula,
                   an.id_partida as id_anulada_por, an.numero as numero_anulada_por,
                   uc.nombre_completo as creado_por, p.creado_en,
                   ua.nombre_completo as aplicada_por, p.aplicada_en,
                   tp.nombre as nombre_tipo
            """;

    private static final String TABLAS = """
              from tb_partidas p
              join tb_tipos_partida tp on tp.codigo = p.codigo_tipo
              join tb_usuarios uc on uc.id_usuario = p.id_usuario_crea
              left join tb_usuarios ua on ua.id_usuario = p.id_usuario_aplica
              left join tb_partidas pa on pa.id_partida = p.id_partida_anula
              left join tb_partidas an on an.id_partida_anula = p.id_partida
              left join lateral (
                    select sum(d.debe) as total_debe, sum(d.haber) as total_haber
                      from tb_partida_detalle d
                     where d.id_partida = p.id_partida
              ) t on true
            """;

    private static final String FILTROS = """
             where (cast(:anio as smallint) is null or p.anio = cast(:anio as smallint))
               and (cast(:mes as smallint) is null or p.mes = cast(:mes as smallint))
               and (cast(:tipo as text) is null or p.codigo_tipo = cast(:tipo as text))
               and (cast(:estado as text) is null or p.estado = cast(:estado as text))
               and (cast(:buscar as text) is null
                    or p.numero like '%' || cast(:buscar as text) || '%'
                    or unaccent(lower(p.concepto)) like '%' || unaccent(lower(cast(:buscar as text))) || '%')
            """;

    private final JdbcClient jdbc;

    public long contar(Filtro f) {
        return conFiltro(jdbc.sql("select count(*) " + TABLAS + FILTROS), f).query(Long.class).single();
    }

    public List<PartidaListaDTO> pagina(Filtro f, int pagina, int tamano) {
        return conFiltro(jdbc.sql(COLUMNAS + TABLAS + FILTROS + """
                         order by p.fecha desc, p.id_partida desc
                         limit :tamano offset :saltar
                        """), f)
                .param("tamano", tamano)
                .param("saltar", (long) pagina * tamano)
                .query((rs, n) -> partida(rs))
                .list();
    }

    public Optional<Encabezado> encabezado(int idPartida) {
        return jdbc.sql(COLUMNAS + TABLAS + " where p.id_partida = :id")
                .param("id", idPartida)
                .query((rs, n) -> new Encabezado(partida(rs), rs.getString("nombre_tipo")))
                .optional();
    }

    public List<LineaPartidaDTO> lineas(int idPartida) {
        return jdbc.sql("""
                        select d.linea, d.codigo_cuenta, c.nombre, d.concepto, d.debe, d.haber
                          from tb_partida_detalle d
                          join tb_cuentas_contables c on c.codigo = d.codigo_cuenta
                         where d.id_partida = :id
                         order by d.linea
                        """)
                .param("id", idPartida)
                .query((rs, n) -> new LineaPartidaDTO(
                        rs.getShort("linea"),
                        rs.getString("codigo_cuenta"),
                        rs.getString("nombre"),
                        rs.getString("concepto"),
                        rs.getBigDecimal("debe"),
                        rs.getBigDecimal("haber")))
                .list();
    }

    public String aplicar(int idPartida, int idUsuario) {
        return jdbc.sql("select fn_aplicar_partida(:id, :usuario)")
                .param("id", idPartida)
                .param("usuario", idUsuario)
                .query(String.class)
                .single();
    }

    public int anular(int idPartida, int idUsuario, String motivo, LocalDate fecha) {
        return jdbc.sql("select fn_anular_partida(:id, :usuario, cast(:motivo as varchar), :fecha)")
                .param("id", idPartida)
                .param("usuario", idUsuario)
                .param("motivo", motivo)
                .param("fecha", fecha)
                .query(Integer.class)
                .single();
    }

    public record Encabezado(PartidaListaDTO partida, String nombreTipo) {}

    private static JdbcClient.StatementSpec conFiltro(JdbcClient.StatementSpec sql, Filtro f) {
        return sql.param("anio", f.anio())
                .param("mes", f.mes())
                .param("tipo", f.tipo())
                .param("estado", f.estado())
                .param("buscar", f.buscar());
    }

    private static PartidaListaDTO partida(ResultSet rs) throws SQLException {
        return new PartidaListaDTO(
                rs.getInt("id_partida"),
                rs.getShort("anio"),
                rs.getShort("mes"),
                rs.getString("numero"),
                rs.getObject("fecha", LocalDate.class),
                rs.getString("codigo_tipo"),
                rs.getString("concepto"),
                rs.getString("estado"),
                rs.getString("origen"),
                rs.getBigDecimal("total_debe"),
                rs.getBigDecimal("total_haber"),
                rs.getObject("id_partida_anula", Integer.class),
                rs.getString("numero_anula"),
                rs.getObject("id_anulada_por", Integer.class),
                rs.getString("numero_anulada_por"),
                rs.getString("creado_por"),
                fecha(rs, "creado_en"),
                rs.getString("aplicada_por"),
                fecha(rs, "aplicada_en"));
    }

    private static OffsetDateTime fecha(ResultSet rs, String columna) throws SQLException {
        OffsetDateTime valor = rs.getObject(columna, OffsetDateTime.class);
        return valor == null ? null : Fechas.local(valor).truncatedTo(ChronoUnit.SECONDS);
    }
}
