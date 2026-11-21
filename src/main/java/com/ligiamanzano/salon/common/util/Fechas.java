package com.ligiamanzano.salon.common.util;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public final class Fechas {

    public static final ZoneId ZONA = ZoneId.of("America/El_Salvador");

    private Fechas() {}

    public static OffsetDateTime local(Instant instante) {
        return instante == null ? null : instante.atZone(ZONA).toOffsetDateTime();
    }
}
