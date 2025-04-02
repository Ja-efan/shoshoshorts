package com.sss.backend.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/redis")
@RequiredArgsConstructor
public class RedisTestController {

    private final RedisTemplate<String, Object> redisTemplate;

    // 저장
    @PostMapping("/set")
    public String setValueTest(@RequestParam String key, @RequestParam String value) {
        redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(60)); // TTL 60초
        return "Saved: " + key + " = " + value;
    }

    // 조회
    @GetMapping("/get")
    public String getValue(@RequestParam String key) {
        Object value = redisTemplate.opsForValue().get(key);
        return value != null ? value.toString() : " No value found";
    }
}
