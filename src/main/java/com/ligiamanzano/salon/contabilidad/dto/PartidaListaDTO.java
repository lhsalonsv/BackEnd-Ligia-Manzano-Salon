package com.ligiamanzano.salon.contabilidad.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record PartidaListaDTO(
        int idPartida,
        short anio,
        short mes,
        String numero,
        LocalDate fecha,
        String codigoTipo,
        String concepto,
        String estado,
        String origen,
        BigDecimal totalDebe,
        BigDecimal totalHaber,
        Integer idPartidaAnula,
        String numeroAnula,
        Integer idPartidaAnuladaPor,
        String numeroAnuladaPor,
        String creadoPor,
        OffsetDateTime creadoEn,
        String aplicadaPor,
        OffsetDateTime aplicadaEn
) {}
