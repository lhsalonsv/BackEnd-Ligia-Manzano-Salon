package com.ligiamanzano.salon.librosiva.dto;

import com.ligiamanzano.salon.configuracion.dto.EmisorEncabezadoDTO;

import java.util.List;
import java.util.Map;

public record LibrosIvaDTO(
        String periodo,
        EmisorEncabezadoDTO emisor,
        List<Map<String, Object>> ventasContribuyentes,
        List<Map<String, Object>> consumidorFinal,
        List<Map<String, Object>> compras,
        List<Map<String, Object>> anulados,
        PendientesDTO pendientes
) {}
