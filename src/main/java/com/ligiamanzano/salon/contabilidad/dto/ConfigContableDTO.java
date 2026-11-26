package com.ligiamanzano.salon.contabilidad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ConfigContableDTO(
        String ctaUtilidadEjercicio,
        String ctaUtilidadesAnteriores,
        String ctaPerdidaEjercicio,
        String ctaPerdidasAnteriores,
        String ctaPerdidasGanancias,
        String ctaReservaLegal,
        String ctaClientes,
        String ctaProveedores,
        @NotBlank(message = "Elige un tipo de partida activo") String tipoPartidaVentas,
        @NotBlank(message = "Elige un tipo de partida activo") String tipoPartidaCompras,
        @NotBlank(message = "Elige Detalle o Resumen") @Pattern(regexp = "^(DETALLE|RESUMEN)$", message = "Elige Detalle o Resumen") String modoVentas,
        @NotBlank(message = "Elige Detalle o Resumen") @Pattern(regexp = "^(DETALLE|RESUMEN)$", message = "Elige Detalle o Resumen") String modoCompras
) {}
