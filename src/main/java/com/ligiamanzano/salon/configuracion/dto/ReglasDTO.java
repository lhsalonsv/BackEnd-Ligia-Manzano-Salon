package com.ligiamanzano.salon.configuracion.dto;

import java.math.BigDecimal;
import java.util.List;

public record ReglasDTO(
        List<HorarioSalonDTO> horarios,
        BigDecimal comisionServicioPct,
        BigDecimal comisionProducto,
        BigDecimal isssEmpleadoPct,
        BigDecimal isssTopeMensual,
        BigDecimal afpEmpleadoPct,
        List<FormaPagoDTO> formasPago,
        UltimoCambioDTO ultimoCambio
) {}
