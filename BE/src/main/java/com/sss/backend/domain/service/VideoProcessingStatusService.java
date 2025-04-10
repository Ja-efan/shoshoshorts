package com.sss.backend.domain.service;

import com.sss.backend.domain.entity.Video.VideoStatus;
import com.sss.backend.domain.entity.VideoProcessingStep;
import com.sss.backend.domain.event.ProcessingStepChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class VideoProcessingStatusService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ApplicationEventPublisher eventPublisher;
    
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
        
        // 이전 단계 조회 (로깅 목적)
        String prevValue = stringRedisTemplate.opsForValue().get(key);
        VideoProcessingStep prevStep = prevValue != null ? VideoProcessingStep.valueOf(prevValue) : null;
        
        // 새 단계 저장
        stringRedisTemplate.opsForValue().set(key, step.name());
        stringRedisTemplate.expire(key, STATUS_TTL_DAYS, TimeUnit.DAYS);
        
        // 디버깅 로그 추가
        log.info("비디오 처리 상태 업데이트: storyId={}, prevStep={}, newStep={}", storyId, prevStep, step.name());
        
        // 단계가 변경되면 이벤트를 발행합니다
        if (prevStep == null || !prevStep.equals(step)) {
            // 처리 단계 변경 이벤트 발행
            eventPublisher.publishEvent(new ProcessingStepChangedEvent(storyId, step));
        }
        
        // 업데이트 후 바로 확인 (디버깅용)
        String savedValue = stringRedisTemplate.opsForValue().get(key);
        log.info("Redis에 저장된 상태 확인: storyId={}, savedValue={}", storyId, savedValue);
    }
    
    /**
     * 비디오의 현재 처리 단계를 조회합니다
     */
    public VideoProcessingStep getProcessingStep(String storyId) {
        String key = VIDEO_PROCESSING_STATUS_KEY_PREFIX + storyId;
        String value = stringRedisTemplate.opsForValue().get(key);
        
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
        stringRedisTemplate.delete(key);
    }
}