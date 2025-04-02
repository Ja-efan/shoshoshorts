package com.sss.backend.config;


import com.sss.backend.jwt.JWTFilter;
import com.sss.backend.jwt.JWTUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
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

    private final JWTUtil jwtUtil;

    public Securityconfig(JWTUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        //CORS 설정
        return http.cors(withDefaults())  // corsConfigurationSource 빈을 자동으로 사용
                .csrf(csrf -> csrf.disable()) // CSRF 비활성화

                // CORS 설정
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // From 로그인 비활성화
                .formLogin((auth) -> auth.disable())

                // HTTP Basic 인증 비활성화
                .httpBasic((auth) -> auth.disable())

                // 세션을 만들지 않도록 설정
                .sessionManagement((session) -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 인증/인가 설정
                .authorizeHttpRequests(auth -> configureAuthorization(auth))

                // JWTFilter를 Spring Security 필터 체인 앞에 추가
                .addFilterBefore(new JWTFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * 인증/인가 경로 설정
     * 허용된 요청 제외 모든 요청 인증 필요
     */
    private void configureAuthorization(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        auth
                .requestMatchers("/", "/api/auth/oauth", "/api/auth/check", "/api/auth/refresh", "/api/youtube/auth", "/api/youtube/auth/callback", "/api/youtube/auth/validate").permitAll()
                .requestMatchers(HttpMethod.POST,"/api/youtube/upload","/api/youtube/auth/logout").permitAll()
                .anyRequest().authenticated();
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