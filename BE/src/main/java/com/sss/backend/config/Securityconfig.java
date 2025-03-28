package com.sss.backend.config;


import com.sss.backend.domain.service.CustomOauth2UserService;
import com.sss.backend.jwt.JWTFilter;
import com.sss.backend.oauth2.CustomSuccessHandler;
import com.sss.backend.jwt.JWTUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration // 설정 파일
@Slf4j
@EnableWebSecurity // Spring Security 활성화
public class Securityconfig {

    private final CustomOauth2UserService customOauth2UserService;
    private final CustomSuccessHandler customSuccessHandler;
    private final JWTUtil jwtUtil;

    public Securityconfig(CustomOauth2UserService customOauth2UserService, CustomSuccessHandler customSuccessHandler, JWTUtil jwtUtil) {
        this.customOauth2UserService = customOauth2UserService;
        this.customSuccessHandler = customSuccessHandler;
        this.jwtUtil = jwtUtil;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        //CORS 설정
        http.cors(withDefaults())  // corsConfigurationSource 빈을 자동으로 사용
            .csrf(csrf -> csrf.disable());

        //From 로그인 방식 disable (OAuth2만 쓸거임)
        http.formLogin((auth) -> auth.disable());

        //HTTP Basic 인증 방식 disable
        http.httpBasic((auth) -> auth.disable());

        //JWTFilter 추가 : Spring Seucurity의 인증 필터 앞에 커스텀 JWT 필터르 끼워넣음
        // 요청 헤더에서 JWT 토큰이 있는지 검사해서 사용자 인증 처리..
        http
                .addFilterBefore(new JWTFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class);

//        //oauth2 로그인 설정 (Spring Security에서 자동으로 OAuth 인증 흐름 처리)
//        http
//                .oauth2Login((oauth2) -> oauth2
//                        .userInfoEndpoint((userInfoEndpointConfig -> userInfoEndpointConfig
//                                .userService(customOauth2UserService)))
//                        .successHandler(customSuccessHandler)); //

        //경로별 인가 작업
        http
                .authorizeHttpRequests((auth) -> auth
                        .requestMatchers("/", "/api/auth/oauth").permitAll() //루트 경로는 모두 접근 허용
                        .anyRequest().authenticated());     // 그 외는 인증 필요함

        //세션 설정 : STATELESS // 사용 안함
        http
                .sessionManagement((session) -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000", "http://localhost:5173","http://shoshoshorts.duckdns.org",
                "http://localhost:63342" // ← IntelliJ에서 HTML 열릴 때 이 주소로 열림
                //Todo 개발 후 삭제
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
//        configuration.setAllowedHeaders(Arrays.asList("Content-Type", "Authorization", "X-Requested-With"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L); //preflight 요청 캐싱 1시간

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); //모든 경로에 위에서 정의한 CORS 설정을 적용
        return source;
    }


}
/**
 * @EnableWebSecurity 내의 Security Filter Chain 을 자동으로 찾아서 적용함.
 *
 */