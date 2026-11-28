package com.ligiamanzano.salon.contabilidad.repository;

import com.ligiamanzano.salon.contabilidad.dto.LineaContableDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ContabilizarRepository {

    private final JdbcClient jdbc;

    public List<LineaContableDTO> pendientes(String origen, LocalDate desde, LocalDate hasta) {
        return jdbc.sql("""
                        select p.grupo, p.fecha, p.tipo_documento, p.id_documento, p.documento,
                               p.codigo_cuenta, c.nombre as nombre_cuenta, p.falta, p.monto
                          from v_contable_pendientes p
                          left join tb_cuentas_contables c on c.codigo = p.codigo_cuenta
                         where p.origen = :origen and p.fecha between :desde and :hasta
                         order by p.fecha, p.grupo, p.tipo_documento, p.id_documento, p.monto desc, p.codigo_cuenta
                        """)
                .param("origen", origen)
                .param("desde", desde)
                .param("hasta", hasta)
                .query((rs, n) -> new LineaContableDTO(
                        rs.getString("grupo"),
                        rs.getObject("fecha", LocalDate.class),
                        rs.getString("tipo_documento"),
                        rs.getInt("id_documento"),
                        rs.getString("documento"),
                        rs.getString("codigo_cuenta"),
                        rs.getString("nombre_cuenta"),
                        rs.getString("falta"),
                        rs.getBigDecimal("monto")))
                .list();
    }

    public List<Integer> contabilizar(String origen, LocalDate desde, LocalDate hasta, int idUsuario) {
        return jdbc.sql("select unnest(fn_contabilizar(:origen, :desde, :hasta, :usuario))")
                .param("origen", origen)
                .param("desde", desde)
                .param("hasta", hasta)
                .param("usuario", idUsuario)
                .query(Integer.class)
                .list();
    }

    public int generarImpuestos(short anio, short mes, int idUsuario) {
        return jdbc.sql("select fn_generar_impuestos_mes(cast(:anio as smallint), cast(:mes as smallint), :usuario)")
                .param("anio", anio)
                .param("mes", mes)
                .param("usuario", idUsuario)
                .query(Integer.class)
                .single();
    }
}
