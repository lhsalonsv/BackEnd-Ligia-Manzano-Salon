package com.ligiamanzano.salon.clientes.dto;

import java.util.List;
import java.util.Map;

public record ClientesPaginaDTO(List<ClienteListaDTO> contenido, long totalElementos, Map<String, Long> conteos) {}
