package com.ligiamanzano.salon.clientes.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record ClienteFichaDTO(
        Integer idCliente,
        String nombre,
        String apellido,
        String telefono,
        String correo,
        Short diaCumple,
        Short mesCumple,
        String alergias,
        String tipoDocumento,
        String numDocumento,
        String razonSocial,
        String nit,
        String nrc,
        String nombreComercial,
        String codActividad,
        Integer idDistrito,
        String complemento,
        boolean activo,
        OffsetDateTime creadoEn,
        LocalDate ultimaVisita,
        int visitas12m,
        BigDecimal gastado12m,
        String empleadoFrecuente,
        FormulaResumenDTO ultimaFormula,
        List<VisitaDTO> ultimasVisitas,
        ProximaCitaDTO proximaCita
) {}
