package com.ligiamanzano.salon.configuracion.service;

import com.ligiamanzano.salon.configuracion.dto.EmisorEncabezadoDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmisorService {

    private final JdbcClient jdbc;

    public EmisorEncabezadoDTO encabezado() {
        return jdbc.sql("""
                        select nombre,
                               regexp_replace(nit, '[^0-9]', '', 'g') as nit,
                               regexp_replace(nrc, '[^0-9]', '', 'g') as nrc
                          from tb_emisores
                         order by id_emisor
                         limit 1
                        """)
                .query((rs, n) -> new EmisorEncabezadoDTO(rs.getString("nombre"), rs.getString("nit"), rs.getString("nrc")))
                .optional()
                .orElse(null);
    }
}
