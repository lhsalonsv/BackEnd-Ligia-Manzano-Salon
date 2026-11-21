package com.ligiamanzano.salon.compras.dto;

import java.util.List;
import java.util.Map;

public record ComprasPaginaDTO(
        List<CompraListaDTO> contenido,
        long totalElementos,
        Map<String, Long> conteos,
        TotalComprasDTO mes
) {}
