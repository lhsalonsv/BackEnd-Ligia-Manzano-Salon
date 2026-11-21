package com.ligiamanzano.salon.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "seguridad")
public record SeguridadProperties(
        String jwtSecret,
        Duration sesionAbsoluta,
        Duration inactividad,
        int intentosMaximos,
        Duration bloqueo,
        List<String> corsOrigenes
) {}
