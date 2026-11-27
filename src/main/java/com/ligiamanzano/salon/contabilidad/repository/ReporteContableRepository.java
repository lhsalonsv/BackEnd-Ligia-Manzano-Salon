package com.ligiamanzano.salon.contabilidad.repository;

import com.ligiamanzano.salon.contabilidad.dto.FilaMayorDTO;
import com.ligiamanzano.salon.contabilidad.dto.SaldoCuentaDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ReporteContableRepository {

    public record FilaDiario(int idPartida, String numero, LocalDate fecha, String codigoTipo, String tipo, String concepto,
                             short linea, String codigoCuenta, String nombreCuenta, String conceptoLinea,
                             BigDecimal debe, BigDecimal haber) {}

    private final JdbcClient jdbc;

    public List<SaldoCuentaDTO> saldos(short anio, short mes, short nivel) {
        return jdbc.sql("select * from fn_saldos_cuentas(cast(:anio as smallint), cast(:mes as smallint), cast(:nivel as smallint))")
                .param("anio", anio)
                .param("mes", mes)
                .param("nivel", nivel)
                .query((rs, n) -> new SaldoCuentaDTO(
                        rs.getString("codigo"),
                        rs.getString("nombre"),
                        rs.getString("tipo"),
                        rs.getString("naturaleza"),
                        rs.getShort("nivel"),
                        rs.getBoolean("acepta_movimiento"),
                        rs.getBigDecimal("saldo_inicial"),
                        rs.getBigDecimal("debe"),
                        rs.getBigDecimal("haber"),
                        rs.getBigDecimal("saldo_final")))
                .list();
    }

    public List<FilaMayorDTO> mayor(String cuenta, LocalDate desde, LocalDate hasta) {
        return jdbc.sql("select * from fn_mayor_cuenta(cast(:cuenta as varchar), :desde, :hasta)")
                .param("cuenta", cuenta)
                .param("desde", desde)
                .param("hasta", hasta)
                .query((rs, n) -> new FilaMayorDTO(
                        rs.getInt("fila"),
                        rs.getObject("fecha", LocalDate.class),
                        rs.getObject("id_partida", Integer.class),
                        rs.getString("numero"),
                        rs.getString("codigo_tipo"),
                        rs.getString("codigo_cuenta"),
                        rs.getString("concepto"),
                        rs.getBigDecimal("debe"),
                        rs.getBigDecimal("haber"),
                        rs.getBigDecimal("saldo")))
                .list();
    }

    public List<FilaDiario> diario(LocalDate desde, LocalDate hasta, String tipo) {
        return jdbc.sql("""
                        select id_partida, numero, fecha, codigo_tipo, tipo, concepto, linea, codigo_cuenta, nombre_cuenta,
                               concepto_linea, debe, haber
                          from v_libro_diario
                         where fecha between :desde and :hasta
                           and (cast(:tipo as text) is null or codigo_tipo = cast(:tipo as text))
                         order by fecha, codigo_tipo, numero, linea
                        """)
                .param("desde", desde)
                .param("hasta", hasta)
                .param("tipo", tipo)
                .query((rs, n) -> new FilaDiario(
                        rs.getInt("id_partida"),
                        rs.getString("numero"),
                        rs.getObject("fecha", LocalDate.class),
                        rs.getString("codigo_tipo"),
                        rs.getString("tipo"),
                        rs.getString("concepto"),
                        rs.getShort("linea"),
                        rs.getString("codigo_cuenta"),
                        rs.getString("nombre_cuenta"),
                        rs.getString("concepto_linea"),
                        rs.getBigDecimal("debe"),
                        rs.getBigDecimal("haber")))
                .list();
    }
}
