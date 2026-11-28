package com.ligiamanzano.salon.compras.repository;

import com.ligiamanzano.salon.common.util.Fechas;
import com.ligiamanzano.salon.compras.dto.CompraResumenDTO;
import com.ligiamanzano.salon.compras.dto.ProveedorListaDTO;
import com.ligiamanzano.salon.compras.dto.TotalComprasDTO;
import com.ligiamanzano.salon.compras.dto.UltimaCompraDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ProveedorConsultaRepository {

    private final JdbcClient jdbc;

    public List<ProveedorListaDTO> lista() {
        return jdbc.sql("""
                        select p.id_proveedor, p.nombre_comercial, p.razon_social, p.nit, p.nrc, p.nacionalidad,
                               p.contacto_nombre, p.telefono, p.dias_credito, p.activo, u.fecha_compra, u.total
                          from tb_proveedores p
                          left join lateral (
                                select c.fecha_compra, c.total
                                  from tb_compras c
                                 where c.id_proveedor = p.id_proveedor and c.estado <> 'ANULADA'
                                 order by c.fecha_compra desc, c.id_compra desc
                                 limit 1
                          ) u on true
                         order by coalesce(p.nombre_comercial, p.razon_social), p.id_proveedor
                        """)
                .query((rs, n) -> {
                    LocalDate fecha = rs.getObject("fecha_compra", LocalDate.class);
                    return new ProveedorListaDTO(
                            rs.getInt("id_proveedor"),
                            rs.getString("nombre_comercial"),
                            rs.getString("razon_social"),
                            rs.getString("nit"),
                            rs.getString("nrc"),
                            rs.getString("nacionalidad"),
                            rs.getString("contacto_nombre"),
                            rs.getString("telefono"),
                            rs.getShort("dias_credito"),
                            rs.getBoolean("activo"),
                            fecha == null ? null : new UltimaCompraDTO(fecha, rs.getBigDecimal("total")));
                })
                .list();
    }

    public List<CompraResumenDTO> ultimasCompras(int idProveedor, int cuantas) {
        return jdbc.sql("""
                        select c.id_compra, c.numero_compra, c.fecha_compra, c.tipo_documento_proveedor, c.total,
                               c.fecha_vencimiento_pago,
                               (select count(*) from tb_detalle_compras d
                                 where d.id_compra = c.id_compra and d.id_producto is not null) as productos,
                               (select count(*) from tb_detalle_compras d
                                 where d.id_compra = c.id_compra and d.id_producto is null) as gastos
                          from tb_compras c
                         where c.id_proveedor = :idProveedor and c.estado <> 'ANULADA'
                         order by c.fecha_compra desc, c.id_compra desc
                         limit :cuantas
                        """)
                .param("idProveedor", idProveedor)
                .param("cuantas", cuantas)
                .query((rs, n) -> new CompraResumenDTO(
                        rs.getInt("id_compra"),
                        rs.getString("numero_compra"),
                        rs.getObject("fecha_compra", LocalDate.class),
                        rs.getString("tipo_documento_proveedor"),
                        rs.getInt("productos"),
                        rs.getInt("gastos"),
                        rs.getBigDecimal("total"),
                        rs.getObject("fecha_vencimiento_pago", LocalDate.class)))
                .list();
    }

    public TotalComprasDTO comprasDelAnio(int idProveedor) {
        return jdbc.sql("""
                        select count(*) as cantidad,
                               coalesce(sum(case when tipo_documento_proveedor = 'NC' then -total else total end), 0.00) as total
                          from tb_compras
                         where id_proveedor = :idProveedor and estado <> 'ANULADA'
                           and extract(year from fecha_compra) = extract(year from cast(:hoy as date))
                        """)
                .param("idProveedor", idProveedor)
                .param("hoy", Fechas.hoy())
                .query((rs, n) -> new TotalComprasDTO(rs.getLong("cantidad"), rs.getBigDecimal("total")))
                .single();
    }

    public Optional<String> ultimaCuenta(int idProveedor) {
        return jdbc.sql("""
                        select d.codigo_cuenta
                          from tb_detalle_compras d
                          join tb_compras c on c.id_compra = d.id_compra
                         where c.id_proveedor = :idProveedor and c.estado <> 'ANULADA' and d.codigo_cuenta is not null
                         order by c.fecha_compra desc, c.id_compra desc, d.id_detalle_compra desc
                         limit 1
                        """)
                .param("idProveedor", idProveedor)
                .query(String.class)
                .optional();
    }
}