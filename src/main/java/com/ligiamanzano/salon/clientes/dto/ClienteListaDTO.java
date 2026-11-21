package com.ligiamanzano.salon.clientes.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ClienteListaDTO(
        Integer idCliente,
        String nombre,
        String apellido,
        String telefono,
        boolean tieneAlergia,
        LocalDate ultimaVisita,
        int visitas12m,
        BigDecimal gastado12m,
        ProximaCitaDTO proximaCita
) {}
