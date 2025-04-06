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
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 토큰 가져오기.
        String token = jwtUtil.extractTokenFromRequest(request);

        if (token == null || jwtUtil.isExpired(token)) {
            log.warn("JWT 토큰 없음 또는 만료");
            filterChain.doFilter(request, response); // 인증 없이 다음 필터로 진행
            return;
        }

        try {
//            String username = jwtUtil.getUsername(token);
            String role = jwtUtil.getRole(token);
            String email = jwtUtil.getEmail(token);

            UserDTO userDTO = new UserDTO(email, role);
            CustomOAuth2User principal = new CustomOAuth2User(userDTO);

            Authentication authToken = new UsernamePasswordAuthenticationToken(
                    principal, null, principal.getAuthorities()
            );

            SecurityContextHolder.getContext().setAuthentication(authToken);
            log.info("인증완료 email : {}", email);

        } catch (Exception e) {
            log.error("JWT 인증 실패 {}",e.getMessage());
        }
        filterChain.doFilter(request,response);

    }
}
