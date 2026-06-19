package com.asknehru.auth.service;

import com.asknehru.auth.model.UserAccount;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

    @Value("${auth.jwt.secret}")
    private String jwtSecret;

    @Value("${auth.jwt.issuer:asknehru-auth}")
    private String jwtIssuer;

    @Value("${auth.jwt.access-expiry-seconds:3600}")
    private long accessExpirySeconds;

    @Value("${auth.jwt.refresh-expiry-seconds:1209600}")
    private long refreshExpirySeconds;

    private Key signingKey;

    @PostConstruct
    void init() {
        signingKey = deriveSigningKey(jwtSecret);
    }

    public String generateAccessToken(UserAccount user) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(accessExpirySeconds);

        return Jwts.builder()
                .setSubject(String.valueOf(user.getId()))
                .setIssuer(jwtIssuer)
                .setAudience("asknehru-clients")
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .claim("email", user.getEmail())
                .claim("type", "access")
                .signWith(signingKey)
                .compact();
    }

    public String generateRefreshToken(UserAccount user, String tokenId) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(refreshExpirySeconds);

        return Jwts.builder()
                .setId(tokenId)
                .setSubject(String.valueOf(user.getId()))
                .setIssuer(jwtIssuer)
                .setAudience("asknehru-clients")
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .claim("type", "refresh")
                .signWith(signingKey)
                .compact();
    }

    public Claims parseAndValidate(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .requireIssuer(jwtIssuer)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public long getAccessExpirySeconds() {
        return accessExpirySeconds;
    }

    public long getRefreshExpirySeconds() {
        return refreshExpirySeconds;
    }

    private Key deriveSigningKey(String secret) {
        byte[] bytes;
        try {
            bytes = Decoders.BASE64.decode(secret);
        } catch (RuntimeException ex) {
            bytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        if (bytes.length < 32) {
            throw new IllegalStateException("auth.jwt.secret must be at least 32 bytes");
        }
        return Keys.hmacShaKeyFor(bytes);
    }
}