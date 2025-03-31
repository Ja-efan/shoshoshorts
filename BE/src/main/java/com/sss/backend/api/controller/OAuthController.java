package com.sss.backend.api.controller;

import com.sss.backend.api.dto.OAuth.OAuthLoginRequest;
import com.sss.backend.api.dto.TokenResponse;
import com.sss.backend.domain.entity.UserEntity;
import com.sss.backend.domain.repository.UserRepository;
import com.sss.backend.domain.service.OAuthService;
import com.sss.backend.domain.service.RedisService;
import com.sss.backend.domain.service.TokenService;
import com.sss.backend.jwt.JWTUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@Slf4j
@RequiredArgsConstructor // 생성자 자동주입
@RequestMapping("/api/auth")
public class OAuthController {

    private final OAuthService oAuthService;
    private final JWTUtil jwtUtil;
    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final RedisService redisService;

    /**
     * OAuth 로그인 및 토큰 유효성 검사를 위한 Controller
     */
    @PostMapping("/oauth")
    public ResponseEntity<?> oauthLogin(@RequestBody OAuthLoginRequest request) {
        log.info("post 요청, {}",request);
        return oAuthService.processOAuthLogin(request.getProvider(), request.getCode());
    }

    /**
     * accessToken 유효성 확인용 API
     * @return
     */
    @GetMapping("/check")
    public ResponseEntity<?> checkTokenValidity(HttpServletRequest request){
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
        log.info("유효한 토큰");
        // TRUE 반환
        return ResponseEntity.ok(Map.of("valid",true));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request) {
        log.info("refresh 로직 On. {} ###",request);

//        String refreshToken = jwtUtil.extractTokenFromRequest(request);
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
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자 정보를 찾을 수 없음"));

        // Access / Refresh Token 갱신
        String accessToken = tokenService.createAccessToken(user);
        String newRefreshToken = tokenService.createAndStoreRefreshToken(email);
        ResponseCookie cookie = tokenService.createRefreshTokenCookie(newRefreshToken);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new TokenResponse(accessToken));
    }
}

