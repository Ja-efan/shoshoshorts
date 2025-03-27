package com.sss.backend.oauth2;

import com.sss.backend.api.dto.OAuth.CustomOAuth2User;
import com.sss.backend.jwt.JWTUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

@Component
public class CustomSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JWTUtil jwtUtil;
    @Value("${app.redirect-url}")
    private String refirectUrl;
    public CustomSuccessHandler(JWTUtil jwtUtil){
        this.jwtUtil = jwtUtil;
    }

    // 로그인 성공시 실행되는 메소드
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        // 로그인된 사용자 정보 가져옴
        CustomOAuth2User customUserDetails = (CustomOAuth2User) authentication.getPrincipal();
        String username = customUserDetails.getUsername(); // 사용자 명

        // 권한 정보 가져오기
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        Iterator<? extends GrantedAuthority> iterator = authorities.iterator();

        GrantedAuthority auth = iterator.next();
        String role = auth.getAuthority();

//        String token = jwtUtil.createJwt(username, role, 60*60*60L); // 60시간
        String token = jwtUtil.createJwt(username, role, 300*1000L); // 30초

        // JWT를 쿠키로 저장
        response.addCookie(createCookie("Authorization", token));
        // 로그인 성공 후 FE로 리디렉션
        response.sendRedirect(refirectUrl);
        //Todo : Redirect Page 하드코딩.

    }

    // JWT 토큰 쿠키 만들어주는 메소드
    private Cookie createCookie(String key, String value) {
        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(30); // 쿠키 유효 시간
        cookie.setSecure(true);
        cookie.setPath("/");    //전체 경로에 적용
        cookie.setHttpOnly(true);   //JavaScript에서 접근 불가능
        return cookie;
    }

}
