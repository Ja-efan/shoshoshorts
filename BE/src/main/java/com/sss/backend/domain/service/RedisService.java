package com.sss.backend.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@Slf4j
@RequiredArgsConstructor
public class RedisService {
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Refresh 토큰을 Redis에 저장하는 메소드
     * @param email
     * @param refreshToken
     */
    public void saveToken(String email, String refreshToken) {
        // "refresh:rmsgnl11@naver.com" : refreshToken.... 형태로 redis 저장.
        redisTemplate.opsForValue()
                .set("refresh:"+email, refreshToken,
                        Duration.ofDays(7)); // TTL 7일.
        log.info("refreshToken Redis 저장 완료 : {} - [}",email, refreshToken);
    }

    /**
     * Refresh 토큰을 Redis에서 조회하는 메서드
     * @param email
     * @return
     */
    public String getToken(String email) {
        Object value = redisTemplate.opsForValue().get(email);
        log.info("refreshToken 확인 완료 : {} - [}",email, value);

        // 지금 RestTemplate<String, Object> 을 쓰고 있어서 꺼낼 때 Object 꺼내짐
        // 그래서 String으로 바꿔서 리턴.
        return value != null ? value.toString() : null;

    }

}
