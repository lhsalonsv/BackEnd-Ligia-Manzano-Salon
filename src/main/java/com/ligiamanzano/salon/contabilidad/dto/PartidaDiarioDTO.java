package com.ligiamanzano.salon.contabilidad.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PartidaDiarioDTO(
        int idPartida,
        String numero,
        LocalDate fecha,
        String codigoTipo,
        String tipo,
        String concepto,
        List<LineaDiarioDTO> lineas,
        BigDecimal totalDebe,
        BigDecimal totalHaber
) {}
