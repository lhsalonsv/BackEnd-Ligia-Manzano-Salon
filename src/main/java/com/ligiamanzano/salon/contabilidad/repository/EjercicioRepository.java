package com.ligiamanzano.salon.contabilidad.repository;

import com.ligiamanzano.salon.common.util.Fechas;
import com.ligiamanzano.salon.contabilidad.dto.EjercicioDTO;
import com.ligiamanzano.salon.contabilidad.dto.PeriodoDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Repository
@RequiredArgsConstructor
public class EjercicioRepository {

    private final JdbcClient jdbc;

    public List<EjercicioDTO> lista(Short anio) {
        Map<Short, List<PeriodoDTO>> periodos = new HashMap<>();
        jdbc.sql("""
                        select anio, mes, estado, cerrado_en
                          from tb_periodos_contables
                         where cast(:anio as smallint) is null or anio = cast(:anio as smallint)
                         order by anio, mes
                        """)
                .param("anio", anio)
                .query(rs -> {
                    OffsetDateTime cerrado = rs.getObject("cerrado_en", OffsetDateTime.class);
                    periodos.computeIfAbsent(rs.getShort("anio"), a -> new ArrayList<>()).add(new PeriodoDTO(
                            rs.getShort("mes"),
                            rs.getString("estado"),
                            cerrado == null ? null : Fechas.local(cerrado).truncatedTo(ChronoUnit.SECONDS)));
                });
        return jdbc.sql("""
                        select anio, fecha_inicio, fecha_fin, estado
                          from tb_ejercicios
                         where cast(:anio as smallint) is null or anio = cast(:anio as smallint)
                         order by anio desc
                        """)
                .param("anio", anio)
                .query((rs, n) -> new EjercicioDTO(
                        rs.getShort("anio"),
                        rs.getObject("fecha_inicio", LocalDate.class),
                        rs.getObject("fecha_fin", LocalDate.class),
                        rs.getString("estado"),
                        periodos.getOrDefault(rs.getShort("anio"), List.of())))
                .list();
    }

    public boolean existe(short anio) {
        return jdbc.sql("select exists (select 1 from tb_ejercicios where anio = :anio)")
                .param("anio", anio)
                .query(Boolean.class)
                .single();
    }

    public void crear(short anio) {
        jdbc.sql("select fn_crear_ejercicio(cast(:anio as smallint))")
                .param("anio", anio)
                .query((rs, n) -> 0)
                .list();
    }

    public void cerrarPeriodo(short anio, short mes, int idUsuario) {
        jdbc.sql("select fn_cerrar_periodo(cast(:anio as smallint), cast(:mes as smallint), :usuario)")
                .param("anio", anio)
                .param("mes", mes)
                .param("usuario", idUsuario)
                .query((rs, n) -> 0)
                .list();
    }

    public void reabrirPeriodo(short anio, short mes) {
        jdbc.sql("select fn_reabrir_periodo(cast(:anio as smallint), cast(:mes as smallint))")
                .param("anio", anio)
                .param("mes", mes)
                .query((rs, n) -> 0)
                .list();
    }

    public int generarPartidaCierre(short anio, int idUsuario) {
        return jdbc.sql("select fn_generar_partida_cierre(cast(:anio as smallint), :usuario)")
                .param("anio", anio)
                .param("usuario", idUsuario)
                .query(Integer.class)
                .single();
    }

    public Integer cerrarEjercicio(short anio, int idUsuario) {
        return jdbc.sql("select fn_cerrar_ejercicio(cast(:anio as smallint), :usuario)")
                .param("anio", anio)
                .param("usuario", idUsuario)
                .query((rs, n) -> rs.getObject(1, Integer.class))
                .single();
    }
}
