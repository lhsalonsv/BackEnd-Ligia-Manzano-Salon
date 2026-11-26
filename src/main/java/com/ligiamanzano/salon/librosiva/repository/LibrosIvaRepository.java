package com.ligiamanzano.salon.librosiva.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class LibrosIvaRepository {

    public enum Anexo {
        VENTAS_CONTRIBUYENTES("v_anexo_ventas_contribuyentes"),
        CONSUMIDOR_FINAL("v_anexo_ventas_consumidor_final"),
        COMPRAS("v_anexo_compras"),
        ANULADOS("v_anexo_anulados");

        private final String vista;

        Anexo(String vista) {
            this.vista = vista;
        }
    }

    private final JdbcClient jdbc;

    public List<Map<String, Object>> filas(Anexo anexo, LocalDate periodo) {
        return jdbc.sql("select * from " + anexo.vista + " where periodo = :periodo order by correlativo")
                .param("periodo", periodo)
                .query((rs, n) -> fila(rs))
                .list();
    }

    public long sinSello(LocalDate desde, LocalDate hasta) {
        return jdbc.sql("""
                        select count(*)
                          from tb_dtes
                         where ambiente = '01' and estado = 'CONTINGENCIA'
                           and fec_emi >= :desde and fec_emi < :hasta
                        """)
                .param("desde", desde)
                .param("hasta", hasta)
                .query(Long.class)
                .single();
    }

    private static Map<String, Object> fila(ResultSet rs) throws SQLException {
        ResultSetMetaData columnas = rs.getMetaData();
        Map<String, Object> fila = new LinkedHashMap<>();
        for (int i = 1; i <= columnas.getColumnCount(); i++) {
            String columna = columnas.getColumnLabel(i);
            if (columna.equals("periodo")) continue;
            Object valor = rs.getObject(i);
            fila.put(camelCase(columna), valor instanceof Date fecha ? fecha.toLocalDate() : valor);
        }
        return fila;
    }

    private static String camelCase(String columna) {
        StringBuilder texto = new StringBuilder();
        boolean mayuscula = false;
        for (char c : columna.toCharArray()) {
            if (c == '_') {
                mayuscula = true;
            } else {
                texto.append(mayuscula ? Character.toUpperCase(c) : c);
                mayuscula = false;
            }
        }
        return texto.toString();
    }
}
