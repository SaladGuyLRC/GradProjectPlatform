package com.projecthelper.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtService {
    private final JwtProperties properties;

    public String create(String userId) {
        Instant now = Instant.now();
        return Jwts.builder().subject(userId).issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(properties.getExpireSeconds())))
                .signWith(key()).compact();
    }

    public String parseUserId(String token) {
        Claims claims = Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();
        return claims.getSubject();
    }

    private SecretKey key() {
        return Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
