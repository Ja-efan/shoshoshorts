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

    public void saveToken(String email, String refreshToken) {
        // "refresh:rmsgnl11@naver.com" : refreshToken.... 형태로 redis 저장.
        redisTemplate.opsForValue()
                .set("refresh:"+email, refreshToken,
                        Duration.ofDays(7)); // TTL 7일.
        log.info("refreshToken Redis 저장 완료 : {} - [}",email, refreshToken);
    }

    public Object getToken(String email) {
        Object value = redisTemplate.opsForValue().get(email);
        log.info("refreshToken 확인 완료 : {} - [}",email, value);
        return value;

    }

}
