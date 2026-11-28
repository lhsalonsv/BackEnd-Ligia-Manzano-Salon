package com.ligiamanzano.salon.contabilidad.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LineaContableDTO(
        String grupo,
        LocalDate fecha,
        String tipoDocumento,
        int idDocumento,
        String documento,
        String codigoCuenta,
        String nombreCuenta,
        String falta,
        BigDecimal monto
) {}
