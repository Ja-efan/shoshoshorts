package com.sss.backend.api.controller;

import com.sss.backend.api.dto.OAuth.OAuthLoginRequest;
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

    /**
     * OAuth 로그인 및 토큰 유효성 검사를 위한 Controller
     */
    @PostMapping("/oauth")
    public ResponseEntity<?> oauthLogin(@RequestBody OAuthLoginRequest request) {
        log.info("post 요청, {}",request);
        String jwt = oAuthService.processOAuthLogin(request.getProvider(), request.getCode());
        log.info("jwt return {}",jwt);
        return ResponseEntity.ok(Map.of("accessToken", jwt));
    }

    /**
     * accessToken 유효성 확인용 API
     * @return
     */
    @GetMapping("/check")
    public ResponseEntity<?> checkTokenValidity(HttpServletRequest request){
        String token = jwtUtil.extractTokenFromRequest(request);

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
}
