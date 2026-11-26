package com.ligiamanzano.salon.inventario.repository;

import com.ligiamanzano.salon.inventario.dto.MovimientoRegistroDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class RegistroInventarioRepository {

    public record Encabezado(int idProducto, String codigo, String nombreProducto, String marca, String unidadMedida,
                             long unidadesIniciales, BigDecimal valorInicial) {}

    public record Fila(int idProducto, MovimientoRegistroDTO movimiento) {}

    private static final String DEL_PRODUCTO =
            "(cast(:idProducto as integer) is null or id_producto = cast(:idProducto as integer))";

    private final JdbcClient jdbc;

    public List<Encabezado> productos(LocalDate desde, LocalDate hasta, Integer idProducto) {
        return jdbc.sql("""
                        with saldos as (
                            select distinct on (id_producto) id_producto, saldo_unidades, saldo_valor
                              from v_registro_inventario
                             where fecha < :desde and %1$s
                             order by id_producto, fecha desc, correlativo desc
                        ), del_mes as (
                            select distinct id_producto
                              from v_registro_inventario
                             where fecha >= :desde and fecha < :hasta and %1$s
                        )
                        select p.id_producto, p.codigo, p.nombre_producto, m.nombre_marca, u.nombre as unidad_medida,
                               coalesce(s.saldo_unidades, 0) as unidades, coalesce(s.saldo_valor, 0.00) as valor
                          from tb_productos p
                          left join tb_marcas m on m.id_marca = p.id_marca
                          left join tb_cat_unidades_medida u on u.cod_unidad_medida = p.cod_unidad_medida
                          left join saldos s on s.id_producto = p.id_producto
                         where case when cast(:idProducto as integer) is not null
                                    then p.id_producto = cast(:idProducto as integer)
                                    else p.id_producto in (select id_producto from del_mes)
                                         or coalesce(s.saldo_unidades, 0) <> 0 or coalesce(s.saldo_valor, 0) <> 0
                               end
                         order by p.nombre_producto, p.id_producto
                        """.formatted(DEL_PRODUCTO))
                .param("desde", desde)
                .param("hasta", hasta)
                .param("idProducto", idProducto)
                .query((rs, n) -> new Encabezado(
                        rs.getInt("id_producto"),
                        rs.getString("codigo"),
                        rs.getString("nombre_producto"),
                        rs.getString("nombre_marca"),
                        rs.getString("unidad_medida"),
                        rs.getLong("unidades"),
                        rs.getBigDecimal("valor")))
                .list();
    }

    public List<Fila> movimientos(LocalDate desde, LocalDate hasta, Integer idProducto) {
        return jdbc.sql("""
                        select *
                          from v_registro_inventario
                         where fecha >= :desde and fecha < :hasta and %s
                         order by id_producto, fecha, correlativo
                        """.formatted(DEL_PRODUCTO))
                .param("desde", desde)
                .param("hasta", hasta)
                .param("idProducto", idProducto)
                .query((rs, n) -> new Fila(rs.getInt("id_producto"), new MovimientoRegistroDTO(
                        rs.getLong("id_movimiento"),
                        rs.getLong("correlativo"),
                        rs.getObject("fecha", LocalDate.class),
                        rs.getString("tipo"),
                        rs.getString("descripcion_operacion"),
                        rs.getString("tipo_documento"),
                        rs.getString("numero_documento"),
                        rs.getString("proveedor"),
                        rs.getString("nacionalidad_proveedor"),
                        rs.getString("referencia_costo"),
                        rs.getInt("id_lote"),
                        rs.getBigDecimal("costo_unitario"),
                        rs.getInt("entrada_unidades"),
                        rs.getInt("salida_unidades"),
                        rs.getBigDecimal("entrada_valor"),
                        rs.getBigDecimal("salida_valor"),
                        rs.getLong("saldo_unidades"),
                        rs.getBigDecimal("saldo_valor"))))
                .list();
    }
}
