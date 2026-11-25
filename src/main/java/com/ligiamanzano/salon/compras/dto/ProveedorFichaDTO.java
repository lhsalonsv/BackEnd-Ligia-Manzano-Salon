package com.ligiamanzano.salon.compras.dto;

import java.util.List;

public record ProveedorFichaDTO(
        Integer idProveedor,
        String razonSocial,
        String nombreComercial,
        String nit,
        String nrc,
        String nacionalidad,
        String contactoNombre,
        String telefono,
        String correo,
        String direccion,
        Short diasCredito,
        String notas,
        boolean activo,
        List<CompraResumenDTO> ultimasCompras,
        TotalComprasDTO comprasAnio
) {}
