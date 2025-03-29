package com.sss.backend.domain.service;

import com.sss.backend.domain.entity.UserEntity;
import com.sss.backend.domain.repository.UserRepository;
import com.sss.backend.jwt.JWTUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    public String processOAuthLogin(String provider, String code){
        log.info("ProcessOAuthLogin ########");

        if (!"google".equals(provider)) throw new RuntimeException("지원하지 않는 provider");

        //1. access_token 요청
        String tokenUri = "https://oauth2.googleapis.com/token";

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", System.getenv("GOOGLE_CLIENT_ID"));
        params.add("client_secret", System.getenv("GOOGLE_CLIENT_SECRET"));
        params.add("code", code);
        params.add("redirect_uri",System.getenv("REDIRECT_URL"));
        // Todo 하드코딩
        log.info("Access token 요청 param : {}",params);

        HttpHeaders headers = new HttpHeaders(); // HTTP 요청을 구성하는 객체
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(tokenUri, request, Map.class);
        // Todo : webclient로 교체하기
        String accessToken = (String) response.getBody().get("access_token");

        log.info("받아온 accessToken : {}",accessToken);

        if (accessToken == null) {
            throw new RuntimeException("access_token 발급 실패");
        }

        //2. 사용자 정보 요청
        log.info("사용자 정보 요청 시작");
        HttpHeaders userHeaders = new HttpHeaders();
        userHeaders.setBearerAuth(accessToken);
        HttpEntity<?> userRequest = new HttpEntity<>(userHeaders);

        ResponseEntity<Map> userInfoResponse = restTemplate.exchange(
                "https://www.googleapis.com/oauth2/v2/userinfo",
                HttpMethod.GET,
                userRequest,
                Map.class
        );

        Map userInfo = userInfoResponse.getBody();
        log.info("userInfo : {}",userInfo);
        String email = (String) userInfo.get("email");
        String name = (String) userInfo.get("name");
        log.info("email and name: {}", email);
        log.info("email and name: {}", name);
        // 유저 DB에서 조회, 없으면 회원 가입
        UserEntity user = userRepository.findByEmail(email).orElseGet(() -> {
            log.info("등록된 유저가 없습니다. 새로운 User 생성");
            UserEntity newUser = new UserEntity(email, name, "ROLE_USER",provider);
            return userRepository.save(newUser);
        });

        //3. JWT 발급
        return jwtUtil.createJwt(user.getEmail(), user.getRole(), user.getProvider(), 20*60*1000L);

    }
}
