package com.ligiamanzano.salon.inventario.dto;

import com.ligiamanzano.salon.configuracion.dto.EmisorEncabezadoDTO;

import java.util.List;

public record RegistroInventarioDTO(EmisorEncabezadoDTO emisor, String periodo, List<ProductoRegistroDTO> productos) {}
