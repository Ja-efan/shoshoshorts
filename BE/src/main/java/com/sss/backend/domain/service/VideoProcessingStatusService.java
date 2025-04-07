package com.sss.backend.domain.service;

import com.sss.backend.domain.entity.VideoProcessingStep;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class VideoProcessingStatusService {

    private final RedisTemplate<String, String> videoStatusRedisTemplate;
    
    // 비디오 처리 상태를 위한 Redis 키 접두사
    private static final String VIDEO_PROCESSING_STATUS_KEY_PREFIX = "video:processing:";
    
    // Redis에 상태 데이터 유지 기간 (1일)
    private static final long STATUS_TTL_DAYS = 1;
    
    private static final Logger log = LoggerFactory.getLogger(VideoProcessingStatusService.class);
    
    /**
     * 비디오의 현재 처리 단계를 저장합니다
     */
    public void updateProcessingStep(String storyId, VideoProcessingStep step) {
        String key = VIDEO_PROCESSING_STATUS_KEY_PREFIX + storyId;
        videoStatusRedisTemplate.opsForValue().set(key, step.name());
        videoStatusRedisTemplate.expire(key, STATUS_TTL_DAYS, TimeUnit.DAYS);
        
        // 디버깅 로그 추가
        log.info("비디오 처리 상태 업데이트: storyId={}, step={}", storyId, step.name());
        
        // 업데이트 후 바로 확인 (디버깅용)
        String savedValue = videoStatusRedisTemplate.opsForValue().get(key);
        log.info("Redis에 저장된 상태 확인: storyId={}, savedValue={}", storyId, savedValue);
    }
    
    /**
     * 비디오의 현재 처리 단계를 조회합니다
     */
    public VideoProcessingStep getProcessingStep(String storyId) {
        String key = VIDEO_PROCESSING_STATUS_KEY_PREFIX + storyId;
        String value = videoStatusRedisTemplate.opsForValue().get(key);
        
        log.info("Redis에서 처리 상태 조회: storyId={}, value={}", storyId, value);
        
        if (value == null) {
            return null;
        }
        
        return VideoProcessingStep.valueOf(value);
    }
    
    /**
     * 비디오의 처리 단계 정보를 삭제합니다
     */
    public void deleteProcessingStep(String storyId) {
        String key = VIDEO_PROCESSING_STATUS_KEY_PREFIX + storyId;
        videoStatusRedisTemplate.delete(key);
    }
}