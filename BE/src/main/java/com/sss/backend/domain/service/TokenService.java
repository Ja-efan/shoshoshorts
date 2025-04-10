package com.sss.backend.domain.service;

import com.sss.backend.api.dto.TokenResponse;
import com.sss.backend.domain.entity.Users;
import com.sss.backend.domain.repository.UserRepository;
import com.sss.backend.jwt.JWTUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * JWT access/refresh Token 발급 및 저장, 쿠키 생성 서비스
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TokenService {
    private final JWTUtil jwtUtil;
    private final RedisService redisService;
    private final UserRepository userRepository;

    /**
     * 액세스 토큰 생성 메서드
     * @param user 사용자 정보(이메일, 역할, provider)
     * @return 생성된 Access Token 문자열
     */
    public String createAccessToken(Users user) {
        return jwtUtil.createAccessToken(
                user.getEmail(),
                user.getRole(),
                user.getProvider(),
                30*60*1000L // 10분.
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
                .secure(false) // 로컬 테스트 시 false로
                .build();
        //todo : 배포시 secure true로 변경
    }

    public ResponseEntity<?> refreshToken(HttpServletRequest request) {
        String refreshToken = jwtUtil.extractRefreshTokenFromCookie(request);
        log.info("refreshToken {}", refreshToken );

        if (refreshToken == null || jwtUtil.isExpired(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("RefreshToken이 null입니다. 유효하지 않습니다");
        }

        String email = jwtUtil.getEmail(refreshToken);

        // Redis에 저장된 refresh 토큰과 비교
        String savedRefresheToken = redisService.getToken("refresh:"+email);
        if (savedRefresheToken == null || !savedRefresheToken.equals(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("RefreshToken이 유효하지 않습니다.");
        }

        // 사용자 정보 조회
        Users user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자 정보를 찾을 수 없음"));

        // Access / Refresh Token 갱신
        String accessToken = createAccessToken(user);
        String newRefreshToken = createAndStoreRefreshToken(email);
        ResponseCookie cookie = createRefreshTokenCookie(newRefreshToken);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new TokenResponse(accessToken));
    }

    public ResponseEntity<?> logoutToken(HttpServletRequest request) {
        String refreshToken = jwtUtil.extractRefreshTokenFromCookie(request);

        // Refresh 토큰 제거 : Redis에서 refresh:{email} 키 삭제
        if (refreshToken != null || !jwtUtil.isExpired(refreshToken)) {
            String email = jwtUtil.getEmail(refreshToken);
            redisService.deleteToken(email);
        }

        // HttpOnly 쿠키 삭제 : 프론트에 Set-Cookie 응답 보내서 refresh 쿠키를 빈값으로 설정
        ResponseCookie deleteCookie = ResponseCookie.from("refreshToken","")
                .httpOnly(true) // JS에서 접근 불가
                .secure(false)   // https일때만 쿠키 전송
                .path("/")
                .maxAge(60*60)
                .sameSite("Lax")
                .build();
        // todo : 배포시 secure true로 바꿔주자.

        return ResponseEntity.ok(Map.of("message","로그아웃 완료"));
    }

    public ResponseEntity<?> validiateToken(HttpServletRequest request) {
        String token = jwtUtil.extractTokenFromRequest(request);
        log.info("token : {}",token);

        // 토큰 유효성 검사
        if (token == null || jwtUtil.isExpired(token) ) {
            log.warn("유효하지 않은 토큰 또는 만료된 토큰");
            // UNAUTHORIZED 반환.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "valid",false,"message",
                            "Invalid or expired token")
                    );
        }
        log.info("유효한 토큰입니다.");
        // TRUE 반환
        return ResponseEntity.ok(Map.of("valid",true));
    }
}
