package com.sss.backend.domain.service;

import com.sss.backend.api.dto.OAuth.UpdateProfileDTO;
import com.sss.backend.api.dto.TokenResponse;
import com.sss.backend.domain.entity.Users;
import com.sss.backend.domain.repository.UserRepository;
import com.sss.backend.jwt.JWTUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class OAuthService {
    private final JWTUtil jwtUtil;
    private final RestTemplate restTemplate = new RestTemplate();
    private final UserRepository userRepository ;
    private final TokenService tokenService;


    @Value("${oauth2.redirect.google}")
    private String googleRedirectUri;

    @Value("${oauth2.redirect.naver}")
    private String naverRedirectUri;

    @Value("${oauth2.redirect.kakao}")
    private String kakaoRedirectUri;

    public void updateProfile(String email, UpdateProfileDTO dto) {
        Users user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("유저정보를 찾을 수 없습니다."));

        // 정보 수정
        if (dto.getNickname() != null) user.setNickname(dto.getNickname());
        if (dto.getPhoneNumber() != null) user.setPhoneNumber(dto.getPhoneNumber());
//        if (dto.getYoutubeToken() != null) user.setNickname(dto.getYoutubeToken());
//        if (dto.getInstagramToken() != null) user.setNickname(dto.getInstagramToken());
//        if (dto.getTiktokToken() != null) user.setNickname(dto.getTiktokToken());

        userRepository.save(user);
        log.info("업데이트 된 정보 : {}",user);

    }


    public ResponseEntity<?> processOAuthLogin(String provider, String code){
        log.info(" ### OAuth Login - Provider : {} ",provider);

        // OAuth 관련 URI, key 설정용 변수 초기화
        String tokenUri = "";
        String userInfoUri ="";
        String clientId = "";
        String clientSecret = "";
        String redirectUri ="";

        // 0. Provider 별 설정 분기
        switch (provider.toLowerCase()) {
            case "google" -> {
                tokenUri = "https://oauth2.googleapis.com/token";
                userInfoUri = "https://www.googleapis.com/oauth2/v2/userinfo";
                clientId = System.getenv("GOOGLE_CLIENT_ID");
                clientSecret = System.getenv("GOOGLE_CLIENT_SECRET");
                redirectUri = googleRedirectUri;
            }
            case "naver" -> {
                tokenUri = "https://nid.naver.com/oauth2.0/token";
                userInfoUri = "https://openapi.naver.com/v1/nid/me";
                clientId = System.getenv("NAVER_CLIENT_ID");
                clientSecret = System.getenv("NAVER_CLIENT_SECRET");
                redirectUri = naverRedirectUri;

            }
            case "kakao" -> {
                tokenUri = "https://kauth.kakao.com/oauth/token";
                userInfoUri = "https://kapi.kakao.com/v2/user/me";
                clientId = System.getenv("KAKAO_CLIENT_ID");
                redirectUri=kakaoRedirectUri;
                // 추가 예정

            }
            // 예외처리
            default -> throw new RuntimeException("지원하지 않는 provider : " + provider);
        }

        // 1. access_token 요청
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("code", code);
        params.add("redirect_uri", redirectUri);

        log.info("Access token 요청 param : {}",params);

        HttpHeaders headers = new HttpHeaders(); // HTTP 요청을 구성하는 객체
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        // 실제 토큰 요청
        ResponseEntity<Map> response = restTemplate.postForEntity(tokenUri, request, Map.class);
        // Todo : webclient로 교체하기


        String authToken = (String) response.getBody().get("access_token");
        log.info("받아온 authToken : {}",authToken);

        if (authToken == null) {
            throw new RuntimeException("access_token 발급 실패");
        }

        // 2. 사용자 정보 요청
        log.info("사용자 정보 요청 시작");
        HttpHeaders userHeaders = new HttpHeaders();
        userHeaders.setBearerAuth(authToken);
        HttpEntity<?> userRequest = new HttpEntity<>(userHeaders);
        // 요청.
        ResponseEntity<Map> userInfoResponse = restTemplate.exchange(
                userInfoUri,
                HttpMethod.GET,
                userRequest,
                Map.class
        );

        Map userInfo = userInfoResponse.getBody();
        log.info("userInfo : {}",userInfo);
        if (userInfo == null) throw new RuntimeException("사용자 정보 조회 실패 // Provider : " + provider);

        // 3. 사용자 정보 추출 (provider 별 분기)
        final String email; // Lambda 함수 내에서 변수값 변경하려면 final 로 선언
        final String name;  // 1회만 초기화.
        final String picture_url;
        switch (provider.toLowerCase()) {
            case "google" -> {
                email = (String) userInfo.get("email");
                name = (String) userInfo.get("name");
            }
            //Todo : 구현 예정
            case "naver" -> {
                Map<String, Object> responseData = (Map<String, Object>) userInfo.get("response");
                log.info("NAVER response : {}",responseData);
                email = (String) responseData.get("email");
                name = (String) responseData.get("name");
            }
            case "kakao" -> {
                log.info("KAKAO response : {}",userInfo);
                Map<String, Object> kakaoAccount = (Map<String, Object>) userInfo.get("kakao_account");
                Map<String, Object> kakaoProfile = (Map<String, Object>) kakaoAccount.get("profile");
                email = (String) kakaoAccount.get("email");
                name = (String) kakaoProfile.get("nickname");
            }
            default -> {
                throw new RuntimeException("지원 X");
            }
        }
        log.info("사용자 이메일: {}, 이름: {}", email, name);

        Users user;

        // 4. 유저 DB에서 조회, 없으면 회원 가입
        try {
            user = userRepository.findByEmail(email).orElseGet(() -> {
                log.info("등록된 유저가 없습니다. 새로운 User 생성");
                Users newUser = new Users(email, name, "ROLE_USER", provider);

                // 닉네임 생성 및 추가
                String nickname = generateRandomNickanme();
                newUser.setNickname(nickname);
                return userRepository.save(newUser);
            });
        } catch (DataIntegrityViolationException e ) { // 무결성을 위반했을 때 발생하는 예외.
            // 이미 누군가 저장했다면
            log.info("DB에 이미 존재함 : {}",e.getMessage());
            user = userRepository.findByEmail(email).orElseThrow();
        }

        // 5. 토큰 생성
        String accessToken = tokenService.createAccessToken(user);
        String refreshToken = tokenService.createAndStoreRefreshToken(user.getEmail());
        ResponseCookie refreshCookie = tokenService.createRefreshTokenCookie(refreshToken);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(new TokenResponse(accessToken));
    }

    /**
     * 닉네임 자동 메소드
     */
    private String generateRandomNickanme() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String timestamp = LocalDateTime.now().format(formatter);
        return "user_" + timestamp;

    }
}