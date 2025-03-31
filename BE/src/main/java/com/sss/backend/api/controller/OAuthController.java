package com.sss.backend.api.controller;

import com.sss.backend.api.dto.OAuth.OAuthLoginRequest;
import com.sss.backend.domain.entity.UserEntity;
import com.sss.backend.domain.repository.UserRepository;
import com.sss.backend.domain.service.OAuthService;
import com.sss.backend.jwt.JWTUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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
        log.info("refresh 로직 On. ###");
        log.info("reqeust {}",request);

//        String refreshToken = jwtUtil.extractTokenFromRequest(request);
        String refreshToken = jwtUtil.extractRefreshTokenFromCookie(request);
        log.info("refreshToken {}", refreshToken );
        log.info("refresh 로직 On. ###");

        if (refreshToken == null || jwtUtil.isExpired(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("RefreshToken이 유효하지 않습니다");
        }

        String email = jwtUtil.getEmail(refreshToken);

        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자 정보를 찾을 수 없음"));

        // Todo : Refresh 토큰 redis 에 저장된 Key-Value와 비교해 유효성검증
        // 맞지 않으면, return.

        String newAccessToken = jwtUtil.createAccessToken(
                user.getEmail(),
                user.getRole(),
                user.getProvider(),
                20 * 60 * 1000L // 20분
        );

        return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
    }
}

