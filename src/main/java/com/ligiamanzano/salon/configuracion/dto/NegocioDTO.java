package com.ligiamanzano.salon.configuracion.dto;

import java.time.LocalDate;

public record NegocioDTO(
        String nombreComercial,
        String nombre,
        String nit,
        String nrc,
        String codActividad,
        String tipoEstablecimiento,
        Integer idDistrito,
        String complemento,
        String telefono,
        String correo,
        String codEstable,
        String codPuntoVenta,
        String ambiente,
        LocalDate certificadoVence,
        UltimoEnvioDTO ultimoEnvio,
        UltimoCambioDTO ultimoCambio
) {}
