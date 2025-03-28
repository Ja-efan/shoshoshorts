package com.sss.backend.jwt;

import com.sss.backend.api.dto.OAuth.CustomOAuth2User;
import com.sss.backend.api.dto.OAuth.UserDTO;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
@Slf4j
public class JWTFilter extends OncePerRequestFilter {
    /**
     * Spring Security에서 JWT 인증을 처리하는 커스텀 필드
     */

    private final JWTUtil jwtUtil;

    // JWTUtil 주입
    public JWTFilter(JWTUtil jwtUtil){
        this.jwtUtil = jwtUtil;
    }

    // 요청마다 실행되는 필터 로직
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        // 쿠키 배열 가져오기.
        String authorization = null;
        Cookie[] cookies = request.getCookies();

        // 쿠키 중 "Authorization" 이름을 가진 쿠키 찾기 => JWT 추출.
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals("Authorization")){
                authorization = cookie.getValue(); // JWT 토큰 저장
            }
        }

        // Authorization 헤더 검증
        if (authorization == null){
            log.info("token null");
            filterChain.doFilter(request, response);

            // 조건이 해당 되면 메소드 종료
            return;
        }

        // 토큰 유효성 검사
        String token = authorization;

        // 토큰 소멸 시간 검증
        if (jwtUtil.isExpired(token)) {
            log.info("token expired");
            filterChain.doFilter(request,response);
            return;
        }

        // 토큰이 유효하면 >> username과 role (사용자 정보) 추출
        String username = jwtUtil.getUsername(token);
        String role = jwtUtil.getRole(token);

        // userDTO 생성하여 값 세팅
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername(username);
        userDTO.setRole(role);

        // UserDTO 기반으로 CustomOAuth2User 생성 (UserDetails 구현체)
        CustomOAuth2User customOAuth2User = new CustomOAuth2User(userDTO);

        // 스프링 시큐리티 인증 토큰(객체) 생성
        Authentication authToken = new UsernamePasswordAuthenticationToken(
                customOAuth2User,
                null,
                customOAuth2User.getAuthorities()
        );
        log.info("SecurityContextHolder에 저장될 사용자 정보 {}",customOAuth2User);
        log.info("SecurityContextHolder에 저장될 사용자 권한 {}",customOAuth2User.getAuthorities());

        // 시큐리티 컨텍스트에 사용자 인증 정보 저장
        SecurityContextHolder.getContext().setAuthentication(authToken);

        // 다음 필터로 요청 넘김
        filterChain.doFilter(request,response);
    }
}
