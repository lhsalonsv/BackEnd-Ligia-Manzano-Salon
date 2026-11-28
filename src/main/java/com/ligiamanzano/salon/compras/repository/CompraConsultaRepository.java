package com.ligiamanzano.salon.compras.repository;

import com.ligiamanzano.salon.common.util.Fechas;
import com.ligiamanzano.salon.compras.dto.CompraDetalleDTO;
import com.ligiamanzano.salon.compras.dto.CompraListaDTO;
import com.ligiamanzano.salon.compras.dto.LineaCompraDTO;
import com.ligiamanzano.salon.compras.dto.TotalComprasDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CompraConsultaRepository {

    public enum Filtro {
        TODAS("todas", "true"),
        ESTE_MES("este-mes", "date_trunc('month', c.fecha_compra) = date_trunc('month', cast(:hoy as date))");

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
            return TODAS;
        }
    }

    private static final long LLAVE_NUMERO_COMPRA = 4_201_001L;

    private static final String TABLAS = """
              from tb_compras c
              join tb_proveedores p on p.id_proveedor = c.id_proveedor
            """;

    private static final String LINEAS = """
              cross join lateral (
                    select count(*) filter (where d.id_producto is not null) as productos,
                           coalesce(sum(d.cantidad) filter (where d.id_producto is not null), 0) as unidades,
                           count(*) filter (where d.id_producto is null) as gastos,
                           (array_agg(d.descripcion order by d.id_detalle_compra)
                                filter (where d.id_producto is null))[1] as primer_gasto
                      from tb_detalle_compras d
                     where d.id_compra = c.id_compra
              ) l
            """;

    private static final String FILTROS = """
             where (cast(:buscar as text) is null
                    or unaccent(lower(c.numero_compra)) like '%' || unaccent(lower(cast(:buscar as text))) || '%'
                    or unaccent(lower(coalesce(p.nombre_comercial, '') || ' ' || p.razon_social))
                       like '%' || unaccent(lower(cast(:buscar as text))) || '%')
               and (cast(:idProveedor as integer) is null or c.id_proveedor = cast(:idProveedor as integer))
            """;

    private final JdbcClient jdbc;

    public String siguienteNumero() {
        jdbc.sql("select 1 from pg_advisory_xact_lock(:llave)").param("llave", LLAVE_NUMERO_COMPRA).query(Integer.class).single();
        int ultimo = jdbc.sql("""
                        select coalesce(max(cast(substring(numero_compra from 3) as integer)), 0)
                          from tb_compras
                         where numero_compra ~ '^C-[0-9]+$'
                        """)
                .query(Integer.class)
                .single();
        return "C-%04d".formatted(ultimo + 1);
    }

    public int registrarDevolucion(int idCompra, int idUsuario) {
        return jdbc.sql("select fn_registrar_devolucion_proveedor(:idCompra, :idUsuario)")
                .param("idCompra", idCompra)
                .param("idUsuario", idUsuario)
                .query(Integer.class)
                .single();
    }

    public Map<String, Long> conteos(String buscar, Integer idProveedor) {
        StringBuilder sql = new StringBuilder("select ");
        Filtro[] filtros = Filtro.values();
        for (int i = 0; i < filtros.length; i++) {
            if (i > 0) sql.append(", ");
            sql.append("count(*) filter (where ").append(filtros[i].condicion).append(") as \"").append(filtros[i].clave).append('"');
        }
        sql.append(TABLAS).append(FILTROS);
        return jdbc.sql(sql.toString())
                .param("buscar", buscar)
                .param("idProveedor", idProveedor)
                .param("hoy", Fechas.hoy())
                .query((rs, n) -> {
                    Map<String, Long> conteos = new LinkedHashMap<>();
                    for (Filtro f : filtros) {
                        conteos.put(f.clave, rs.getLong(f.clave));
                    }
                    return conteos;
                })
                .single();
    }

    public List<CompraListaDTO> pagina(String buscar, Integer idProveedor, Filtro filtro, int pagina, int tamano) {
        String sql = """
                select c.id_compra, c.numero_compra, c.fecha_compra, c.id_proveedor,
                       coalesce(p.nombre_comercial, p.razon_social) as nombre_proveedor,
                       c.tipo_documento_proveedor, c.numero_documento_proveedor, c.total, c.estado,
                       l.productos, l.unidades, l.gastos, l.primer_gasto
                """ + TABLAS + LINEAS + FILTROS + "   and " + filtro.condicion + """

                 order by c.fecha_compra desc, c.id_compra desc
                 limit :tamano offset :saltar
                """;
        return jdbc.sql(sql)
                .param("buscar", buscar)
                .param("idProveedor", idProveedor)
                .param("hoy", Fechas.hoy())
                .param("tamano", tamano)
                .param("saltar", (long) pagina * tamano)
                .query((rs, n) -> new CompraListaDTO(
                        rs.getInt("id_compra"),
                        rs.getString("numero_compra"),
                        rs.getObject("fecha_compra", LocalDate.class),
                        rs.getInt("id_proveedor"),
                        rs.getString("nombre_proveedor"),
                        rs.getString("tipo_documento_proveedor"),
                        rs.getString("numero_documento_proveedor"),
                        rs.getInt("productos"),
                        rs.getInt("unidades"),
                        rs.getInt("gastos"),
                        rs.getString("primer_gasto"),
                        rs.getBigDecimal("total"),
                        rs.getString("estado")))
                .list();
    }

    public TotalComprasDTO delMes() {
        return jdbc.sql("""
                        select count(*) as cantidad,
                               coalesce(sum(case when tipo_documento_proveedor = 'NC' then -total else total end), 0.00) as total
                          from tb_compras
                         where estado <> 'ANULADA'
                           and date_trunc('month', fecha_compra) = date_trunc('month', cast(:hoy as date))
                        """)
                .param("hoy", Fechas.hoy())
                .query((rs, n) -> new TotalComprasDTO(rs.getLong("cantidad"), rs.getBigDecimal("total")))
                .single();
    }

    public Optional<CompraDetalleDTO> detalle(int idCompra) {
        List<LineaCompraDTO> lineas = jdbc.sql("""
                        select d.id_detalle_compra, d.id_producto, pr.nombre_producto, d.descripcion, d.cantidad,
                               d.costo_unitario, d.subtotal, d.codigo_cuenta,
                               coalesce(l.cantidad_disponible, dev.cantidad_disponible) as cantidad_disponible
                          from tb_detalle_compras d
                          left join tb_productos pr on pr.id_producto = d.id_producto
                          left join tb_lotes l on l.id_detalle_compra = d.id_detalle_compra
                          left join lateral (
                                select lo.cantidad_disponible
                                  from tb_movimientos_inventario m
                                  join tb_lotes lo on lo.id_lote = m.id_lote
                                 where m.id_detalle_compra = d.id_detalle_compra and m.tipo = 'DEVOLUCION_PROVEEDOR'
                                 limit 1
                          ) dev on true
                         where d.id_compra = :idCompra
                         order by d.id_detalle_compra
                        """)
                .param("idCompra", idCompra)
                .query((rs, n) -> new LineaCompraDTO(
                        rs.getInt("id_detalle_compra"),
                        rs.getObject("id_producto", Integer.class),
                        rs.getString("nombre_producto"),
                        rs.getString("descripcion"),
                        rs.getInt("cantidad"),
                        rs.getBigDecimal("costo_unitario"),
                        rs.getBigDecimal("subtotal"),
                        rs.getObject("cantidad_disponible", Integer.class),
                        rs.getString("codigo_cuenta")))
                .list();
        return jdbc.sql("""
                        select c.*, coalesce(p.nombre_comercial, p.razon_social) as nombre_proveedor,
                               upper(c.codigo_generacion_proveedor::text) as codigo_generacion,
                               r.numero_compra as numero_compra_relacionada, u.nombre_completo
                          from tb_compras c
                          join tb_proveedores p on p.id_proveedor = c.id_proveedor
                          join tb_usuarios u on u.id_usuario = c.id_usuario
                          left join tb_compras r on r.id_compra = c.id_compra_relacionada
                         where c.id_compra = :idCompra
                        """)
                .param("idCompra", idCompra)
                .query((rs, n) -> new CompraDetalleDTO(
                        rs.getInt("id_compra"),
                        rs.getString("numero_compra"),
                        rs.getInt("id_proveedor"),
                        rs.getString("nombre_proveedor"),
                        rs.getObject("fecha_compra", LocalDate.class),
                        rs.getString("tipo_documento_proveedor"),
                        rs.getString("numero_documento_proveedor"),
                        rs.getShort("clase_documento"),
                        rs.getString("codigo_generacion"),
                        rs.getObject("id_compra_relacionada", Integer.class),
                        rs.getString("numero_compra_relacionada"),
                        rs.getObject("periodo_iva", LocalDate.class),
                        rs.getBigDecimal("subtotal_sin_iva"),
                        rs.getBigDecimal("compras_exentas"),
                        rs.getBigDecimal("iva"),
                        rs.getBigDecimal("iva_percibido"),
                        rs.getBigDecimal("total"),
                        rs.getShort("tipo_operacion_renta"),
                        rs.getShort("clasificacion_renta"),
                        rs.getShort("sector_renta"),
                        rs.getShort("tipo_costo_gasto"),
                        rs.getObject("fecha_vencimiento_pago", LocalDate.class),
                        rs.getString("estado"),
                        rs.getString("nombre_completo"),
                        Fechas.local(rs.getObject("creado_en", OffsetDateTime.class)).truncatedTo(ChronoUnit.SECONDS),
                        lineas,
                        rs.getString("codigo_cuenta_exentas")))
                .optional();
    }

    public boolean contabilizada(int idCompra) {
        return jdbc.sql("select fn_contabilizado('COMPRA', :idCompra)")
                .param("idCompra", idCompra)
                .query(Boolean.class)
                .single();
    }
}
