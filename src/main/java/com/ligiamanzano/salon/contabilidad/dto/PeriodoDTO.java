package com.ligiamanzano.salon.contabilidad.dto;

import java.time.OffsetDateTime;

public record PeriodoDTO(short mes, String estado, OffsetDateTime cerradoEn) {}
