package com.ligiamanzano.salon.compras.dto;

public record ProveedorListaDTO(
        Integer idProveedor,
        String nombreComercial,
        String razonSocial,
        String nit,
        String nrc,
        String contactoNombre,
        String telefono,
        Short diasCredito,
        boolean activo,
        UltimaCompraDTO ultimaCompra
) {}
