package com.ligiamanzano.salon.inventario.repository;

import com.ligiamanzano.salon.common.util.Fechas;
import com.ligiamanzano.salon.inventario.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class InventarioFisicoConsultaRepository {

    private static final String PRODUCTO = """
                   p.id_producto, p.codigo, p.nombre_producto, m.nombre_marca,
                   coalesce(c.nombre_categoria, 'Sin categoría') as categoria, u.nombre as unidad_medida
            """;

    private static final String CATALOGOS = """
              left join tb_marcas m on m.id_marca = p.id_marca
              left join tb_categorias c on c.id_categoria = p.id_categoria
              left join tb_cat_unidades_medida u on u.cod_unidad_medida = p.cod_unidad_medida
            """;

    private final JdbcClient jdbc;

    public List<InventarioFisicoListaDTO> lista() {
        return jdbc.sql("""
                        select f.id_inventario_fisico, f.fecha_corte, f.estado, f.creado_en, f.cerrado_en,
                               (select count(*) from tb_inventario_fisico_conteo k
                                 where k.id_inventario_fisico = f.id_inventario_fisico) as productos_contados
                          from tb_inventarios_fisicos f
                         order by f.fecha_corte desc
                        """)
                .query((rs, n) -> new InventarioFisicoListaDTO(
                        rs.getInt("id_inventario_fisico"),
                        rs.getObject("fecha_corte", LocalDate.class),
                        rs.getString("estado"),
                        rs.getLong("productos_contados"),
                        fecha(rs, "creado_en"),
                        fecha(rs, "cerrado_en")))
                .list();
    }

    public List<ConteoProductoDTO> conteo(int idInventarioFisico, boolean cerrado) {
        return jdbc.sql("select " + PRODUCTO + """
                               , coalesce(case when :cerrado then k.existencia_sistema end, e.existencia, 0) as existencia,
                               coalesce(e.valor, 0.00) as valor,
                               k.cantidad_contada, k.nota
                          from tb_productos p
                        """ + CATALOGOS + """
                          left join v_existencia_al_corte e
                                 on e.id_producto = p.id_producto and e.id_inventario_fisico = :id
                          left join tb_inventario_fisico_conteo k
                                 on k.id_producto = p.id_producto and k.id_inventario_fisico = :id
                         where case when :cerrado then k.id_conteo is not null else p.activo or k.id_conteo is not null end
                         order by categoria, p.nombre_producto, p.id_producto
                        """)
                .param("id", idInventarioFisico)
                .param("cerrado", cerrado)
                .query((rs, n) -> conteo(rs))
                .list();
    }

    public int cerrar(int idInventarioFisico, int idUsuario, String observaciones) {
        return jdbc.sql("select fn_cerrar_inventario_fisico(:id, :usuario, cast(:observaciones as varchar))")
                .param("id", idInventarioFisico)
                .param("usuario", idUsuario)
                .param("observaciones", observaciones)
                .query(Integer.class)
                .single();
    }

    public List<ProductoPendienteDTO> sinContar(int idInventarioFisico) {
        return jdbc.sql("""
                        select p.id_producto, p.nombre_producto
                          from tb_productos p
                         where p.activo
                           and not exists (select 1 from tb_inventario_fisico_conteo k
                                            where k.id_producto = p.id_producto and k.id_inventario_fisico = :id)
                         order by p.nombre_producto
                        """)
                .param("id", idInventarioFisico)
                .query((rs, n) -> new ProductoPendienteDTO(rs.getInt("id_producto"), rs.getString("nombre_producto")))
                .list();
    }

    public List<ActaFilaDTO> acta(int idInventarioFisico) {
        return jdbc.sql("""
                        select *
                          from v_acta_inventario_fisico
                         where id_inventario_fisico = :id
                         order by categoria, nombre_producto, id_producto, id_lote
                        """)
                .param("id", idInventarioFisico)
                .query((rs, n) -> new ActaFilaDTO(
                        rs.getString("categoria"),
                        rs.getString("codigo"),
                        rs.getString("nombre_producto"),
                        rs.getString("nombre_marca"),
                        rs.getString("unidad_medida"),
                        rs.getInt("id_lote"),
                        rs.getLong("cantidad"),
                        rs.getBigDecimal("costo_unitario"),
                        rs.getBigDecimal("valor"),
                        rs.getString("referencia_costo")))
                .list();
    }

    public List<AjusteFisicoDTO> ajustes(int idInventarioFisico) {
        return jdbc.sql("""
                        select id_producto, sum(cantidad) as cantidad, round(sum(cantidad * costo_unitario), 2) as valor
                          from tb_movimientos_inventario
                         where id_inventario_fisico = :id
                         group by id_producto
                         order by id_producto
                        """)
                .param("id", idInventarioFisico)
                .query((rs, n) -> new AjusteFisicoDTO(rs.getInt("id_producto"), rs.getLong("cantidad"), rs.getBigDecimal("valor")))
                .list();
    }

    private static ConteoProductoDTO conteo(ResultSet rs) throws SQLException {
        return new ConteoProductoDTO(
                rs.getInt("id_producto"),
                rs.getString("codigo"),
                rs.getString("nombre_producto"),
                rs.getString("nombre_marca"),
                rs.getString("categoria"),
                rs.getString("unidad_medida"),
                rs.getInt("existencia"),
                rs.getBigDecimal("valor"),
                rs.getObject("cantidad_contada", Integer.class),
                rs.getString("nota"));
    }

    private static OffsetDateTime fecha(ResultSet rs, String columna) throws SQLException {
        OffsetDateTime valor = rs.getObject(columna, OffsetDateTime.class);
        return valor == null ? null : Fechas.local(valor).truncatedTo(ChronoUnit.SECONDS);
    }
}
