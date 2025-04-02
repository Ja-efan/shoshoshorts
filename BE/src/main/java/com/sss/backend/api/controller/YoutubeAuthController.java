package com.sss.backend.api.controller;

import com.sss.backend.domain.service.YoutubeAuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/youtube/auth")
public class YoutubeAuthController {

    @Value("${frontend.redirect.url}")
    private String frontendRedirectUrl;

    // 쿠키 설정
    @Value("${youtube.cookie.domain:localhost}")
    private String cookieDomain;

    @Value("${youtube.cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${youtube.cookie.max-age:3600}")
    private int cookieMaxAge;


    private final YoutubeAuthService youtubeAuthService;

    public YoutubeAuthController(YoutubeAuthService youtubeAuthService) {
        this.youtubeAuthService = youtubeAuthService;
    }

    /**
     * Google OAuth 인증 URL을 생성하여 반환하는 엔드포인트
     * 프론트엔드에서 이 URL로 사용자를 리다이렉트하여 Google 로그인을 시작함
     */
    @GetMapping("")
    public ResponseEntity<Map<String, String>> getAuthUrl() {
        String authUrl = youtubeAuthService.generateAuthUrl();
        Map<String, String> response = new HashMap<>();
        response.put("authUrl", authUrl);
        return ResponseEntity.ok(response);
    }

    /**
     * Google OAuth 콜백을 처리하는 엔드포인트
     * 인증 코드를 받아 액세스 토큰으로 교환한 후 프론트엔드로 리다이렉트
     * 토큰은 쿠키로 전달
     */
    @GetMapping("/callback")
    public RedirectView handleCallback(@RequestParam("code") String code, @RequestParam(value = "state", required = false) String state, HttpServletResponse response) {
        try {

            // 인증 코드로 액세스 토큰 획득
            String accessToken = youtubeAuthService.exchangeCodeForToken(code);

            // 토큰을 HTTP Only 쿠키로 저장
            Cookie cookie = new Cookie("youtube_access_token", accessToken);
            cookie.setHttpOnly(true);  // JavaScript에서 접근 불가능하게 설정
            cookie.setPath("/");       // 모든 경로에서 사용 가능
            cookie.setMaxAge(3600);  // 쿠키 만료 시간 (초)

//            // SameSite 속성 설정 (Jakarta Servlet 5.0+ 또는 Spring 3.0+에서 사용 가능)
//            // 이전 버전에서는 헤더를 직접 설정해야 함
//            try {
//                // Jakarta Servlet 5.0+ 환경
//                cookie.setAttribute("SameSite", cookieSameSite);
//            } catch (NoSuchMethodError e) {
//                // 이전 버전 환경에서는 헤더를 직접 설정
//                String cookieHeader = String.format("%s=%s; Path=/; HttpOnly; SameSite=%s",
//                        cookieName, accessToken, cookieSameSite);
//
//                if (!"localhost".equals(cookieDomain)) {
//                    cookieHeader += "; Domain=" + cookieDomain;
//                }
//
//                if (cookieSecure) {
//                    cookieHeader += "; Secure";
//                }
//
//                response.setHeader("Set-Cookie", cookieHeader);
//            }

            response.addCookie(cookie);

            // 인증 성공 정보만 포함하여 프론트엔드로 리다이렉트
            RedirectView redirectView = new RedirectView();
            redirectView.setUrl(frontendRedirectUrl + "?authSuccess=true");
            return redirectView;
        } catch (Exception e) {
            RedirectView redirectView = new RedirectView();
            redirectView.setUrl(frontendRedirectUrl + "?error=" + e.getMessage());
            return redirectView;
        }
    }


//    /**
//     * 토큰 유효성을 검사하는 엔드포인트
//     * Authorization 헤더 또는 쿠키에서 토큰을 가져옴
//     */
//    @PostMapping("/validate")
//    public ResponseEntity<?> validateToken(@RequestHeader(value = "Authorization", required = false) String authHeader,
//                                           @CookieValue(value = "youtube_access_token", required = false) String cookieToken) {
//        try {
//            // 토큰 소스 우선순위: 헤더 > 쿠키
//            String accessToken = null;
//
//            if (authHeader != null && !authHeader.isEmpty()) {
//                accessToken = authHeader.replace("Bearer ", "");
//
////                // 토큰에서 state 파라미터 제거
////                if (accessToken.contains("&state=")) {
////                    accessToken = accessToken.substring(0, accessToken.indexOf("&state="));
////                }
//            } else if (cookieToken != null && !cookieToken.isEmpty()) {
//                accessToken = cookieToken;
//            }
//
//            if (accessToken == null) {
//                throw new IllegalArgumentException("토큰을 찾을 수 없습니다.");
//            }
//
//            boolean isValid = youtubeAuthService.validateToken(accessToken);
//
//            Map<String, Object> response = new HashMap<>();
//            response.put("valid", isValid);
//
//            return ResponseEntity.ok(response);
//        } catch (Exception e) {
//            Map<String, Object> response = new HashMap<>();
//            response.put("valid", false);
//            response.put("message", e.getMessage());
//
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
//        }
//    }

    /**
     * 인증 상태를 확인하는 엔드포인트
     * 쿠키에서 토큰을 읽어 유효성을 검사
     */
    @GetMapping("/validate")
    public ResponseEntity<?> validateAuth(@CookieValue(value = "youtube_access_token", required = false) String accessToken) {
        if (accessToken == null || accessToken.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("authenticated", false, "message", "토큰이 없습니다."));
        }

        boolean isValid = youtubeAuthService.validateToken(accessToken);

        if (isValid) {
            return ResponseEntity.ok(Map.of("authenticated", true));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("authenticated", false, "message", "토큰이 유효하지 않습니다."));
        }
    }


    /**
     * 로그아웃 엔드포인트
     * 쿠키를 만료시켜 로그아웃 처리
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("youtube_access_token", "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0); // 즉시 만료

        response.addCookie(cookie);

        return ResponseEntity.ok(Map.of("message", "로그아웃 되었습니다."));
    }



}
