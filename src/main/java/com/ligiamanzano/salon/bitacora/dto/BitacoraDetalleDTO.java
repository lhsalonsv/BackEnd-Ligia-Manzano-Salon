package com.ligiamanzano.salon.bitacora.dto;

import tools.jackson.databind.JsonNode;

import java.time.OffsetDateTime;

public record BitacoraDetalleDTO(
        Long idBitacora,
        OffsetDateTime fechaHora,
        Integer idUsuario,
        String nombreUsuario,
        String modulo,
        String accion,
        String entidad,
        String entidadId,
        String descripcion,
        JsonNode datosAntes,
        JsonNode datosDespues,
        String equipo
) {}
