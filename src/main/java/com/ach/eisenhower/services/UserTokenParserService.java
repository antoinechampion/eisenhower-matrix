package com.ach.eisenhower.services;

import com.ach.eisenhower.repositories.EisenhowerUserRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.List;

@Component
public class UserTokenParserService {
    private static final long validityInMilliseconds = 30L * 24 * 3600 * 1000; // 1 month
    private final EisenhowerUserRepository userRepository;
    @Value("${eisenhower.jwt.signingkey}")
    private String secretKeyStr;
    private SecretKey secretKey;
    private JwtParser jwtParser;

    public UserTokenParserService(EisenhowerUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostConstruct
    public void init() {
        var secret = Base64.getEncoder().encodeToString(this.secretKeyStr.getBytes());
        secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        jwtParser =  Jwts.parserBuilder().setSigningKey(secretKey).build();
    }

    public String createToken(String username, List<String> roles) {
        Claims claims = Jwts.claims().setSubject(username);
        claims.put("roles", roles);
        Date now = new Date();
        Date validity = new Date(now.getTime() + validityInMilliseconds);
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public Authentication getAuthentication(String token) {
        var userDetails = this.userRepository.findByEmail(getUsername(token));
        if (userDetails.isEmpty()) {
            return null;
        }
        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.get().getAuthorities());
    }

    public String getUsername(String token) {
        return jwtParser.parseClaimsJws(token).getBody().getSubject();
    }

    public String resolveToken(HttpServletRequest req) {
        var cookies = req.getCookies();
        if (cookies != null && cookies.length > 0) {
            var authCookie = Arrays.stream(cookies)
                    .filter((Cookie c) -> c.getName().equals("token"))
                    .findFirst();
            if (authCookie.isPresent()) {
                return authCookie.get().getValue();
            }
        }

        var bearerToken = req.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }
    public boolean validateToken(String token) {
        try {
            Jws<Claims> claims = jwtParser.parseClaimsJws(token);
            return !claims.getBody().getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public static Cookie forgeAuthenticationCookie(String token) {
        var cookie = new Cookie("token", token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge((int) (validityInMilliseconds / 1000 * 0.95));
        return cookie;
    }
}