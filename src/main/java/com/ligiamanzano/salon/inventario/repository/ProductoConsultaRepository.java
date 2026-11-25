package com.ligiamanzano.salon.inventario.repository;

import com.ligiamanzano.salon.common.util.Fechas;
import com.ligiamanzano.salon.inventario.dto.LoteDTO;
import com.ligiamanzano.salon.inventario.dto.MovimientoResumenDTO;
import com.ligiamanzano.salon.inventario.dto.ProductoListaDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
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
public class ProductoConsultaRepository {

    public enum Filtro {
        TODOS("todos", "b.activo"),
        BAJO_MINIMO("bajo-minimo", "b.activo and b.existencia < b.stock_minimo"),
        AGOTADOS("agotados", "b.activo and b.existencia = 0"),
        INACTIVOS("inactivos", "not b.activo");

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

    public record Nombres(String nombreMarca, String nombreCategoria) {}

    private static final String BASE = """
            with base as (
                select p.id_producto, p.codigo, p.nombre_producto, p.stock_minimo, p.precio_venta, p.activo,
                       c.nombre_categoria,
                       coalesce(e.existencia, 0) as existencia,
                       coalesce(e.valor_costo, 0.00) as valor_costo
                  from tb_productos p
                  join tb_categorias c on c.id_categoria = p.id_categoria
                  left join (select l.id_producto,
                                    sum(l.cantidad_disponible) as existencia,
                                    round(sum(l.cantidad_disponible * l.costo_unitario), 2) as valor_costo
                               from tb_lotes l
                              group by l.id_producto) e on e.id_producto = p.id_producto
                 where (cast(:buscar as text) is null
                        or unaccent(lower(p.nombre_producto)) like '%' || unaccent(lower(cast(:buscar as text))) || '%'
                        or lower(p.codigo) like '%' || lower(cast(:buscar as text)) || '%')
                   and (cast(:idCategoria as integer) is null or p.id_categoria = cast(:idCategoria as integer))
            )
            """;

    private static final String COSTO_PEPS = """
            coalesce(
                (select l.costo_unitario from tb_lotes l
                  where l.id_producto = %1$s and l.cantidad_disponible > 0
                  order by l.fecha_ingreso, l.id_lote limit 1),
                (select l.costo_unitario from tb_lotes l
                  where l.id_producto = %1$s
                  order by l.fecha_ingreso desc, l.id_lote desc limit 1))
            """;

    private static final String REFERENCIA = """
            coalesce(c.numero_compra,
                     (select t.numero_control from tb_dtes t
                        join tb_detalle_ventas dv on dv.id_venta = t.id_venta
                       where dv.id_detalle_venta = m.id_detalle_venta
                       order by t.id_dte desc limit 1))
            """;

    private final JdbcClient jdbc;

    public Map<String, Long> conteos(String buscar, Integer idCategoria) {
        StringBuilder sql = new StringBuilder(BASE).append("select ");
        Filtro[] filtros = Filtro.values();
        for (int i = 0; i < filtros.length; i++) {
            if (i > 0) sql.append(", ");
            sql.append("count(*) filter (where ").append(filtros[i].condicion).append(") as \"").append(filtros[i].clave).append('"');
        }
        sql.append(" from base b");
        return jdbc.sql(sql.toString())
                .param("buscar", buscar)
                .param("idCategoria", idCategoria)
                .query((rs, n) -> {
                    Map<String, Long> conteos = new LinkedHashMap<>();
                    for (Filtro f : filtros) {
                        conteos.put(f.clave, rs.getLong(f.clave));
                    }
                    return conteos;
                })
                .single();
    }

    public List<ProductoListaDTO> pagina(String buscar, Integer idCategoria, Filtro filtro, int pagina, int tamano) {
        String sql = BASE + "select b.*, " + COSTO_PEPS.formatted("b.id_producto") + " as costo_peps"
                + " from base b where " + filtro.condicion
                + " order by b.nombre_producto, b.id_producto limit :tamano offset :saltar";
        return jdbc.sql(sql)
                .param("buscar", buscar)
                .param("idCategoria", idCategoria)
                .param("tamano", tamano)
                .param("saltar", (long) pagina * tamano)
                .query((rs, n) -> new ProductoListaDTO(
                        rs.getInt("id_producto"),
                        rs.getString("codigo"),
                        rs.getString("nombre_producto"),
                        rs.getString("nombre_categoria"),
                        rs.getInt("existencia"),
                        rs.getInt("stock_minimo"),
                        rs.getBigDecimal("costo_peps"),
                        rs.getBigDecimal("precio_venta"),
                        rs.getBigDecimal("valor_costo"),
                        rs.getBoolean("activo")))
                .list();
    }

    public BigDecimal valorTotal() {
        return jdbc.sql("select coalesce(round(sum(cantidad_disponible * costo_unitario), 2), 0.00) from tb_lotes")
                .query(BigDecimal.class)
                .single();
    }

    public Nombres nombres(int idProducto) {
        return jdbc.sql("""
                        select m.nombre_marca, c.nombre_categoria
                          from tb_productos p
                          join tb_marcas m on m.id_marca = p.id_marca
                          join tb_categorias c on c.id_categoria = p.id_categoria
                         where p.id_producto = :idProducto
                        """)
                .param("idProducto", idProducto)
                .query((rs, n) -> new Nombres(rs.getString("nombre_marca"), rs.getString("nombre_categoria")))
                .single();
    }

    public Optional<BigDecimal> costoPeps(int idProducto) {
        return jdbc.sql("select " + COSTO_PEPS.formatted(":idProducto"))
                .param("idProducto", idProducto)
                .query((rs, n) -> Optional.ofNullable(rs.getBigDecimal(1)))
                .single();
    }

    public int vendidos30Dias(int idProducto) {
        return jdbc.sql("""
                        select coalesce(-sum(cantidad), 0)
                          from tb_movimientos_inventario
                         where id_producto = :idProducto
                           and tipo in ('SALIDA_VENTA', 'DEVOLUCION_ANULACION', 'DEVOLUCION_NC', 'DEVOLUCION_FACTURA')
                           and fecha_hora >= now() - interval '30 days'
                        """)
                .param("idProducto", idProducto)
                .query(Integer.class)
                .single();
    }

    public List<LoteDTO> lotesConExistencia(int idProducto) {
        return jdbc.sql("""
                        select l.id_lote, l.fecha_ingreso, l.costo_unitario, l.cantidad_inicial, l.cantidad_disponible,
                               c.numero_compra
                          from tb_lotes l
                          left join tb_detalle_compras d on d.id_detalle_compra = l.id_detalle_compra
                          left join tb_compras c on c.id_compra = d.id_compra
                         where l.id_producto = :idProducto and l.cantidad_disponible > 0
                         order by l.fecha_ingreso, l.id_lote
                        """)
                .param("idProducto", idProducto)
                .query((rs, n) -> new LoteDTO(
                        rs.getInt("id_lote"),
                        fecha(rs, "fecha_ingreso"),
                        rs.getBigDecimal("costo_unitario"),
                        rs.getInt("cantidad_inicial"),
                        rs.getInt("cantidad_disponible"),
                        rs.getString("numero_compra")))
                .list();
    }

    public List<MovimientoResumenDTO> ultimosMovimientos(int idProducto, int cuantos) {
        return jdbc.sql("select m.fecha_hora, m.tipo, m.cantidad, m.motivo, " + REFERENCIA + " as referencia" + """
                          from tb_movimientos_inventario m
                          left join tb_detalle_compras d on d.id_detalle_compra = m.id_detalle_compra
                          left join tb_compras c on c.id_compra = d.id_compra
                         where m.id_producto = :idProducto
                         order by m.fecha_hora desc, m.id_movimiento desc
                         limit :cuantos
                        """)
                .param("idProducto", idProducto)
                .param("cuantos", cuantos)
                .query((rs, n) -> new MovimientoResumenDTO(
                        fecha(rs, "fecha_hora"),
                        rs.getString("tipo"),
                        rs.getInt("cantidad"),
                        rs.getString("referencia"),
                        rs.getString("motivo")))
                .list();
    }


    private static OffsetDateTime fecha(ResultSet rs, String columna) throws SQLException {
        OffsetDateTime valor = rs.getObject(columna, OffsetDateTime.class);
        return valor == null ? null : Fechas.local(valor).truncatedTo(ChronoUnit.SECONDS);
    }
}