package com.ligiamanzano.salon.bitacora.dto;

import java.time.OffsetDateTime;

public record BitacoraRegistroDTO(
        Long idBitacora,
        OffsetDateTime fechaHora,
        Integer idUsuario,
        String nombreUsuario,
        String modulo,
        String accion,
        String entidad,
        String entidadId,
        String descripcion
) {}
