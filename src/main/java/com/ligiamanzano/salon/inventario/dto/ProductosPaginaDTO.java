package com.ligiamanzano.salon.inventario.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record ProductosPaginaDTO(
        List<ProductoListaDTO> contenido,
        long totalElementos,
        Map<String, Long> conteos,
        BigDecimal valorTotal,
        BigDecimal comisionProducto,
        BigDecimal ivaPct
) {}
