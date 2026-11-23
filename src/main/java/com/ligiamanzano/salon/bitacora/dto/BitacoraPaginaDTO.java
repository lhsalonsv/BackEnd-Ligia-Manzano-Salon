package com.ligiamanzano.salon.bitacora.dto;

import java.util.List;
import java.util.Map;

public record BitacoraPaginaDTO(
        List<BitacoraRegistroDTO> contenido,
        long totalElementos,
        Map<String, Long> conteos,
        List<PersonaBitacoraDTO> personas
) {}
