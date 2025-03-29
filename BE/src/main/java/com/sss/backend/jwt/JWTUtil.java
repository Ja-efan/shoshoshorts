package com.sss.backend.jwt;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@Slf4j
public class JWTUtil {
    private SecretKey secretKey;

    public JWTUtil(@Value("${spring.jwt.secret}")String secret) {
        // Serect key 기반으로 JWT 서명 키 생성.
        secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), Jwts.SIG.HS256.key().build().getAlgorithm());

    }
    public String getUsername(String token) {

        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().get("username", String.class);
    }

    public String getRole(String token) {

        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().get("role", String.class);
    }

    /**
     * 토큰 만료되었는지 확인하는 메소드
     * 만료되었다면 true 리턴
     */
    public Boolean isExpired(String token) {
        try {
            Date expiration = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getExpiration();

            log.info("expiration : {}",expiration);

            return expiration.before(new Date());

        } catch (ExpiredJwtException e) {
            log.warn("JWT 만료 : {} ", e.getMessage());
            return true;
        } catch (Exception e) {
            log.warn("JWT 파싱 오류 {}", e.getMessage());
            return true;
        }
    }

    // JWT 생성
    public String createJwt(String email, String role, String provider, Long expiredMs) {
        return Jwts.builder()
                .claim("email", email) // payload
                .claim("role", role)
                .claim("provider", provider)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiredMs))
                .signWith(secretKey) // 서명
                .compact(); // 최종 JWT 문자열 생성
    }

    public String extractTokenFromRequest(HttpServletRequest request) {
        // 1. Authorization 헤더
        String bearerToken = request.getHeader("Authorization");

            // Bearer 제외하고 return
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        // 2. 쿠키
            // 토큰만 리턴
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("Authorization".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return null;

    }
}
