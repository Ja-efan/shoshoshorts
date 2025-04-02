package com.sss.backend.domain.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTubeScopes;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.*;

@Service
public class YoutubeAuthService {

    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static HttpTransport HTTP_TRANSPORT;

    @Value("${youtube.application.name}")
    private String applicationName;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    @Value("${youtube.redirect.uri}")
    private String redirectUri;

    private GoogleAuthorizationCodeFlow flow;

    // 유튜브 API 사용을 위한 스코프 설정
    private static final Collection<String> SCOPES = Arrays.asList(
            YouTubeScopes.YOUTUBE,
            YouTubeScopes.YOUTUBE_UPLOAD
    );


    //서비스가 초기화될 때 실행되는 메서드
    @PostConstruct
    public void init() throws IOException, GeneralSecurityException {
        HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport(); //구글 API와 HTTP 통신을 위한 객체 생성

        // 직접 클라이언트 ID와 시크릿으로 플로우 설정
        // OAuth 인증 프로세스를 관리 -> 인증 URL 생성, 토큰 교환
        flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientId, clientSecret, SCOPES)
                .setAccessType("offline")
                .setApprovalPrompt("force") //사용자에게 권한 동의 화면을 표시
                .build();
    }

    /**
     * Google 인증 URL 생성
     * @return 인증 URL 문자열
     */
    public String generateAuthUrl() {

        // 고유한 상태 값 생성 (CSRF 공격 방지)
        String state = UUID.randomUUID().toString();

        System.out.println("state: "+ state);

        // 인증 URL 생성
        String authUrl = flow.newAuthorizationUrl()
                .setRedirectUri(redirectUri) //인증 후 리다이렉트할 URL 설정
                .setState(state) //상태 값 설정 ->  나중에 검증
                .build();

        return authUrl;
    }

    /**
     * 인증 코드를 액세스 토큰으로 교환
     * @param code Google 인증 후 콜백으로 받은 인증 코드
     * @return 액세스 토큰
     */
    public String exchangeCodeForToken(String code) throws IOException {
        TokenResponse tokenResponse = new GoogleAuthorizationCodeTokenRequest(
                HTTP_TRANSPORT,
                JSON_FACTORY,
                clientId,
                clientSecret,
                code,
                redirectUri)
                .execute();

        return tokenResponse.getAccessToken(); //응답으로 받은 객체에서 액세스 토큰 추출
    }

    /**
     * 토큰의 유효성 검사
     * @param accessToken 구글 액세스 토큰
     * @return 유효 여부
     */
    public boolean validateToken(String accessToken) {
        try {

            // 토큰으로 Google Credential(Google API 요청에 인증 정보를 첨부하는 객체) 생성
            // 이 객체는 API 호출 시 HTTP 요청 헤더에 인증 정보 자동으로 추가
            Credential credential = new GoogleCredential().setAccessToken(accessToken); //access토큰 객체에 설정

            // YouTube API 서비스 생성
            YouTube youtube = new YouTube.Builder(
                    new NetHttpTransport(),
                    JacksonFactory.getDefaultInstance(),
                    credential)
                    .setApplicationName(applicationName)
                    .build();

            // 3. 테스트 API 호출: 사용자 자신의 채널 정보 요청
            youtube.channels().list(Collections.singletonList("snippet"))
                    .setMine(true)
                    .execute();

            return true; // 성공적으로 API 호출되면 토큰 유효성 확인됨
        } catch (Exception e) {

            System.out.println("YouTube 토큰 검증 실패: " + e.getMessage());
            e.printStackTrace();
            return false; // API 호출 실패 시 토큰 무효
        }
    }

}
