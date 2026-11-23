package com.ligiamanzano.salon.bitacora.repository;

import com.ligiamanzano.salon.bitacora.dto.BitacoraDetalleDTO;
import com.ligiamanzano.salon.bitacora.dto.BitacoraRegistroDTO;
import com.ligiamanzano.salon.bitacora.dto.PersonaBitacoraDTO;
import com.ligiamanzano.salon.bitacora.service.BitacoraService;
import com.ligiamanzano.salon.common.util.Fechas;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class BitacoraConsultaRepository {

    public record Filtro(OffsetDateTime desde, OffsetDateTime hasta, boolean soloSistema,
                         Integer idUsuario, String buscar) {}

    private static final String COLUMNAS = """
            select b.id_bitacora, b.fecha_hora, b.id_usuario, u.nombre_completo,
                   b.modulo, b.accion, b.entidad, b.entidad_id, b.descripcion
            """;

    private static final String DESDE = """
              from tb_bitacora b
              left join tb_usuarios u on u.id_usuario = b.id_usuario
             where b.fecha_hora >= :desde and b.fecha_hora < :hasta
               and (not :soloSistema or b.id_usuario is null)
               and (cast(:idUsuario as integer) is null or b.id_usuario = cast(:idUsuario as integer))
               and (cast(:buscar as text) is null
                    or unaccent(lower(b.descripcion)) like '%' || unaccent(lower(cast(:buscar as text))) || '%'
                    or unaccent(lower(coalesce(b.entidad_id, ''))) like '%' || unaccent(lower(cast(:buscar as text))) || '%')
            """;

    private final JdbcClient jdbc;
    private final JsonMapper json;

    public Map<String, Long> conteos(Filtro f) {
        Map<String, Long> conteos = new LinkedHashMap<>();
        conteos.put(BitacoraService.TODO, 0L);
        conjunto("select b.modulo, count(*) as cantidad" + DESDE + " group by b.modulo order by count(*) desc, b.modulo", f)
                .query((rs, n) -> Map.entry(rs.getString("modulo"), rs.getLong("cantidad")))
                .list()
                .forEach(e -> {
                    conteos.put(e.getKey(), e.getValue());
                    conteos.merge(BitacoraService.TODO, e.getValue(), Long::sum);
                });
        return conteos;
    }

    public List<BitacoraRegistroDTO> pagina(Filtro f, String modulo, int pagina, int tamano) {
        String sql = COLUMNAS + DESDE + """
                   and (cast(:modulo as text) is null or b.modulo = cast(:modulo as text))
                 order by b.fecha_hora desc, b.id_bitacora desc
                 limit :tamano offset :saltar
                """;
        return conjunto(sql, f)
                .param("modulo", modulo)
                .param("tamano", tamano)
                .param("saltar", (long) pagina * tamano)
                .query((rs, n) -> registro(rs))
                .list();
    }

    public List<PersonaBitacoraDTO> personas(OffsetDateTime desde, OffsetDateTime hasta) {
        return jdbc.sql("""
                        select distinct u.id_usuario, u.nombre_completo
                          from tb_bitacora b
                          join tb_usuarios u on u.id_usuario = b.id_usuario
                         where b.fecha_hora >= :desde and b.fecha_hora < :hasta
                         order by u.nombre_completo
                        """)
                .param("desde", desde)
                .param("hasta", hasta)
                .query((rs, n) -> new PersonaBitacoraDTO(rs.getInt("id_usuario"), rs.getString("nombre_completo")))
                .list();
    }

    public Optional<BitacoraDetalleDTO> detalle(long idBitacora) {
        return jdbc.sql(COLUMNAS + """
                             , b.datos_antes::text as datos_antes, b.datos_despues::text as datos_despues, b.equipo
                          from tb_bitacora b
                          left join tb_usuarios u on u.id_usuario = b.id_usuario
                         where b.id_bitacora = :idBitacora
                        """)
                .param("idBitacora", idBitacora)
                .query((rs, n) -> {
                    BitacoraRegistroDTO r = registro(rs);
                    return new BitacoraDetalleDTO(
                            r.idBitacora(), r.fechaHora(), r.idUsuario(), r.nombreUsuario(),
                            r.modulo(), r.accion(), r.entidad(), r.entidadId(), r.descripcion(),
                            leerJson(rs.getString("datos_antes")),
                            leerJson(rs.getString("datos_despues")),
                            rs.getString("equipo"));
                })
                .optional();
    }

    private JdbcClient.StatementSpec conjunto(String sql, Filtro f) {
        return jdbc.sql(sql)
                .param("desde", f.desde())
                .param("hasta", f.hasta())
                .param("soloSistema", f.soloSistema())
                .param("idUsuario", f.idUsuario())
                .param("buscar", f.buscar());
    }

    private JsonNode leerJson(String texto) {
        return texto == null ? null : json.readTree(texto);
    }

    private static BitacoraRegistroDTO registro(ResultSet rs) throws SQLException {
        return new BitacoraRegistroDTO(
                rs.getLong("id_bitacora"),
                Fechas.local(rs.getObject("fecha_hora", OffsetDateTime.class)).truncatedTo(ChronoUnit.SECONDS),
                rs.getObject("id_usuario", Integer.class),
                rs.getString("nombre_completo"),
                rs.getString("modulo"),
                rs.getString("accion"),
                rs.getString("entidad"),
                rs.getString("entidad_id"),
                rs.getString("descripcion"));
    }
}
