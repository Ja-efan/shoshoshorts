package com.sss.backend.domain.service;

import com.sss.backend.domain.entity.UserEntity;
import com.sss.backend.domain.repository.UserRepository;
import com.sss.backend.jwt.JWTUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class OAuthService {
    private final JWTUtil jwtUtil;
    private final RestTemplate restTemplate = new RestTemplate();
    private final UserRepository userRepository ;

    @Value("${oauth2.redirect.google}")
    private String googleRedirectUri;

    @Value("${oauth2.redirect.naver}")
    private String naverRedirectUri;

    @Value("${oauth2.redirect.kakao}")
    private String kakaoRedirectUri;

    public String processOAuthLogin(String provider, String code){
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
//                redirectUri = System.getenv("GOOGLE_REDIRECT_URL");
                redirectUri = googleRedirectUri;
            }
//            case "naver" -> {
//                tokenUri = "https://nid.naver.com/oauth2.0/token";
//                userInfoUri = "https://openapi.naver.com/v1/nid/me";
//                // 추가 예정
//
//            }
//            case "kakao" -> {
//                tokenUri = "https://kauth.kakao.com/oauth/token";
//                userInfoUri = "https://kapi.kakao.com/v2/user/me";
//                // 추가 예정
//
//            }
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


        String accessToken = (String) response.getBody().get("access_token");
        log.info("받아온 accessToken : {}",accessToken);

        if (accessToken == null) {
            throw new RuntimeException("access_token 발급 실패");
        }

        // 2. 사용자 정보 요청
        log.info("사용자 정보 요청 시작");
        HttpHeaders userHeaders = new HttpHeaders();
        userHeaders.setBearerAuth(accessToken);
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
        switch (provider.toLowerCase()) {
            case "google" -> {
                email = (String) userInfo.get("email");
                name = (String) userInfo.get("name");
            }
//            case "naver" -> {
//                email = "";
//                name = "";
//                //구현예정
//            }
//            case "kakao" -> {
//                email = "";
//                name = "";
//                //구현예정
//            }
            default -> {
                throw new RuntimeException("지원 X");
            }
        }
        log.info("사용자 이메일: {}, 이름: {}", email, name);

        // 4. 유저 DB에서 조회, 없으면 회원 가입
        UserEntity user = userRepository.findByEmail(email).orElseGet(() -> {
            log.info("등록된 유저가 없습니다. 새로운 User 생성");
            UserEntity newUser = new UserEntity(email, name, "ROLE_USER",provider);
            return userRepository.save(newUser);
        });

        //3. JWT 발급
        return jwtUtil.createJwt(user.getEmail(), user.getRole(), user.getProvider(), 20*60*1000L);

    }
}
