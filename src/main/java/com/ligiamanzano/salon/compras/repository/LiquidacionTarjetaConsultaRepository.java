package com.ligiamanzano.salon.compras.repository;

import com.ligiamanzano.salon.compras.dto.LiquidacionTarjetaDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LiquidacionTarjetaConsultaRepository {

    private static final String CONSULTA = """
            select t.*, coalesce(p.nombre_comercial, p.razon_social) as nombre_proveedor, c.nombre as nombre_cuenta_banco,
                   t.codigo_generacion::text as codigo_generacion_texto,
                   fn_contabilizado('LIQUIDACION_TARJETA', t.id_liquidacion) as contabilizada
              from tb_liquidaciones_tarjeta t
              join tb_proveedores p on p.id_proveedor = t.id_proveedor
              join tb_cuentas_contables c on c.codigo = t.codigo_cuenta_banco
            """;

    private final JdbcClient jdbc;

    public List<LiquidacionTarjetaDTO> lista(LocalDate desde, LocalDate hasta) {
        return jdbc.sql(CONSULTA + """
                         where (cast(:desde as date) is null or t.fecha >= cast(:desde as date))
                           and (cast(:hasta as date) is null or t.fecha <= cast(:hasta as date))
                         order by t.fecha desc, t.id_liquidacion desc
                        """)
                .param("desde", desde)
                .param("hasta", hasta)
                .query((rs, n) -> convertirADTO(rs))
                .list();
    }

    public Optional<LiquidacionTarjetaDTO> una(int idLiquidacion) {
        return jdbc.sql(CONSULTA + " where t.id_liquidacion = :id")
                .param("id", idLiquidacion)
                .query((rs, n) -> convertirADTO(rs))
                .optional();
    }

    private static LiquidacionTarjetaDTO convertirADTO(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new LiquidacionTarjetaDTO(
                rs.getInt("id_liquidacion"),
                rs.getObject("fecha", LocalDate.class),
                rs.getInt("id_proveedor"),
                rs.getString("nombre_proveedor"),
                rs.getString("numero_documento"),
                rs.getString("codigo_generacion_texto"),
                rs.getObject("periodo_iva", LocalDate.class),
                rs.getBigDecimal("monto_liquidado"),
                rs.getBigDecimal("anticipo_iva"),
                rs.getBigDecimal("comision_total"),
                rs.getBigDecimal("monto_depositado"),
                rs.getString("codigo_cuenta_banco"),
                rs.getString("nombre_cuenta_banco"),
                rs.getObject("id_compra_comision", Integer.class),
                rs.getBoolean("contabilizada"));
    }
}
