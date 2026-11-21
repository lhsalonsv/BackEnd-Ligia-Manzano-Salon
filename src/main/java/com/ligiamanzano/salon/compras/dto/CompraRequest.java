package com.ligiamanzano.salon.compras.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;

public record CompraRequest(
        @NotNull(message = "Elige el proveedor") Integer idProveedor,
        @NotNull(message = "Escribe la fecha") @PastOrPresent(message = "No puede ser una fecha futura") LocalDate fechaCompra,
        @NotBlank @Pattern(regexp = "^(CCF|FACTURA|OTRO)$", message = "Elige CCF, FACTURA u OTRO") String tipoDocumentoProveedor,
        @Size(max = 40) String numeroDocumentoProveedor,
        @NotEmpty(message = "Agrega al menos un producto") List<@Valid LineaCompraRequest> detalle
) {}
