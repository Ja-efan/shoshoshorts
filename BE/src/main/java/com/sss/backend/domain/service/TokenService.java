package com.sss.backend.domain.service;

import com.sss.backend.domain.entity.UserEntity;
import com.sss.backend.jwt.JWTUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

/**
 * JWT access/refresh Token 발급 및 저장, 쿠키 생성 서비스
 */
@Service
@RequiredArgsConstructor
public class TokenService {
    private final JWTUtil jwtUtil;
    private final RedisService redisService;

    /**
     * 액세스 토큰 생성 메서드
     * @param user 사용자 정보(이메일, 역할, provider)
     * @return 생성된 Access Token 문자열
     */
    public String createAccessToken(UserEntity user) {
        return jwtUtil.createAccessToken(
                user.getEmail(),
                user.getRole(),
                user.getProvider(),
                10*60*1000L // 10분.
        );
    }

    /**
     * Refrsh 토큰 생성 및 Redis 저장 메서드
     * @param email 사용자 이메일 정보
     * @return 생성된 refresh 토큰 문자열
     */
    public String createAndStoreRefreshToken(String email) {
        String refreshToken = jwtUtil.createRefreshToken(email, 7 * 24 * 60 *60*1000L );

        // Redis에 email : refrshTokne형태로 저장
        redisService.saveToken(email, refreshToken);
        return refreshToken;
    }

    /**
     * HttpOnly 쿠키에 저장할 Refresh Token 생성
     * @param refreshToken
     * @return
     */
    public ResponseCookie createRefreshTokenCookie(String refreshToken) {
        return ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .path("/")
                .maxAge(7 * 24 * 60 * 60)
                .sameSite("Lax")
                .secure(true) // 로컬 테스트 시 false로
                .build();
    }
}
