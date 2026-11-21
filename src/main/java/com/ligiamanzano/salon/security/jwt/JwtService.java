package com.ligiamanzano.salon.security.jwt;

import com.ligiamanzano.salon.config.SeguridadProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtService {
    private static final String EMISOR = "salon-api";
    private final SecretKey llave;

    public JwtService(SeguridadProperties props) {
        //Lanza WeakKeyException al arrancar si el secreto tiene menos de 256 bits
        this.llave = Keys.hmacShaKeyFor(Decoders.BASE64.decode(props.jwtSecret()));
    }

    public String generar(int idUsuario, UUID idSesion, Instant expira) {
        return Jwts.builder()
                .issuer(EMISOR)
                .subject(String.valueOf(idUsuario))
                .id(idSesion.toString())
                .issuedAt(new Date())
                .expiration(Date.from(expira))
                .signWith(llave, Jwts.SIG.HS256)
                .compact();
    }

    /** Devuelve los claims o lanza JwtException si la firma, el emisor o la fecha no son válidos. */
    public Claims validar(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(llave)
                .requireIssuer(EMISOR)
                .clockSkewSeconds(30)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
