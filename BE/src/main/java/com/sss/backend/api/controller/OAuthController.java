package com.sss.backend.api.controller;

import com.sss.backend.api.dto.OAuth.OAuthLoginRequest;
import com.sss.backend.api.dto.OAuth.UserInfoDTO;
import com.sss.backend.domain.repository.UserRepository;
import com.sss.backend.domain.service.OAuthService;
import com.sss.backend.domain.service.RedisService;
import com.sss.backend.domain.service.TokenService;
import com.sss.backend.jwt.JWTUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@Slf4j
@RequiredArgsConstructor // 생성자 자동주입
@RequestMapping("/api/auth")
public class OAuthController {

    private final OAuthService oAuthService;
    private final TokenService tokenService;

    /**
     * OAuth 로그인 및 토큰 유효성 검사를 위한 Controller
     */
    @PostMapping("/oauth")
    public ResponseEntity<?> oauthLogin(@RequestBody OAuthLoginRequest request) {
        log.info("로그인/회원가입 요청, {}",request);
        return oAuthService.processOAuthLogin(request.getProvider(), request.getCode());
    }

    /**
     * accessToken 유효성 확인용 API
     */
    @GetMapping("/check")
    public ResponseEntity<?> checkTokenValidity(HttpServletRequest request){
        log.info("Access 토큰 유효성 검사 API 실행");
        return tokenService.validiateToken(request);
    }

    /**
     * 로그아웃 메소드
     * refreshToken 제거 및 쿠키 제거
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request){
        log.info("logout API 실행");
        return tokenService.logoutToken(request);
    }


    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request) {
        log.info("refresh 로직 On. {} ###",request);
        return tokenService.refreshToken(request);

    }

    @GetMapping("/info")
    public ResponseEntity<UserInfoDTO> getUserInfo(HttpServletRequest request) {
        log.info("유저 정보 조회 API 호출");

        return oAuthService.getUserInfo(request);
    }

}

