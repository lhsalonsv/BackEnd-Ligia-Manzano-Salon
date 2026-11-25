package com.ligiamanzano.salon.inventario.repository;

import com.ligiamanzano.salon.inventario.service.Peps.LoteDisponible;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PepsRepository {

    private final JdbcClient jdbc;

    //Para bloquear lotes que participen en una  transacción
    //Solo funciona dentro de una transacción (el bloqueo dura hasta el commit)
    public List<LoteDisponible> bloquearLotesConExistencia(int idProducto) {
        return jdbc.sql("""
                        select id_lote, cantidad_disponible, costo_unitario
                          from tb_lotes
                         where id_producto = :idProducto and cantidad_disponible > 0
                         order by fecha_ingreso, id_lote
                           for update
                        """)
                .param("idProducto", idProducto)
                .query((rs, n) -> new LoteDisponible(
                        rs.getInt("id_lote"), rs.getInt("cantidad_disponible"), rs.getBigDecimal("costo_unitario")))
                .list();
    }

    public int existencia(int idProducto) {
        return jdbc.sql("select coalesce(sum(cantidad_disponible), 0) from tb_lotes where id_producto = :idProducto")
                .param("idProducto", idProducto)
                .query(Integer.class)
                .single();
    }

    //Costo del lote más nuevo
    public Optional<BigDecimal> ultimoCosto(int idProducto) {
        return jdbc.sql("""
                        select costo_unitario
                          from tb_lotes
                         where id_producto = :idProducto
                         order by fecha_ingreso desc, id_lote desc
                         limit 1
                        """)
                .param("idProducto", idProducto)
                .query(BigDecimal.class)
                .optional();
    }
}