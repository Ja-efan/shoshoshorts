package com.sss.backend.domain.service;

import com.sss.backend.api.dto.VideoStatusResponseDto;
import com.sss.backend.domain.entity.Video.VideoStatus;
import com.sss.backend.domain.entity.VideoProcessingStep;
import com.sss.backend.domain.event.ProcessingStepChangedEvent;
import com.sss.backend.domain.repository.SseEmitterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 비디오 처리 상태를 SSE로 전송하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VideoStatusSseService {

    private final SseEmitterRepository sseEmitterRepository;
    private final VideoProcessingStatusService videoProcessingStatusService;
    private final StringRedisTemplate stringRedisTemplate;
    
    // SSE 연결 타임아웃 (30분)
    private static final long SSE_TIMEOUT = 30 * 60 * 1000L;
    
    // Redis 키 접두사
    private static final String VIDEO_STATUS_KEY_PREFIX = "video:status:";
    
    // Redis에 상태 데이터 유지 기간 (1일)
    private static final long STATUS_TTL_DAYS = 1;
    
    // 마지막으로 전송된 처리 단계를 추적하기 위한 Map
    private final Map<String, VideoProcessingStep> lastSentProcessingSteps = new ConcurrentHashMap<>();
    
    /**
     * ProcessingStepChangedEvent 이벤트 처리
     * 비디오 처리 단계가 변경될 때 호출됩니다.
     */
    @EventListener
    public void handleProcessingStepChangedEvent(ProcessingStepChangedEvent event) {
        log.info("처리 단계 변경 이벤트 수신: storyId={}, step={}", event.getStoryId(), event.getStep());
        // SSE로 상태 업데이트 전송
        sendStatusUpdateToClient(event.getStoryId(), VideoStatus.PROCESSING);
    }
    
    /**
     * 비디오 상태를 Redis에 저장
     */
    public void saveVideoStatus(String storyId, VideoStatus status) {
        String key = VIDEO_STATUS_KEY_PREFIX + storyId;
        stringRedisTemplate.opsForValue().set(key, status.name());
        stringRedisTemplate.expire(key, STATUS_TTL_DAYS, TimeUnit.DAYS);
        
        // 상태가 변경되었으므로 즉시 SSE로 상태 전송
        sendStatusUpdateToClient(storyId, status);
    }
    
    /**
     * Redis에서 비디오 상태 조회
     */
    public VideoStatus getVideoStatusFromRedis(String storyId) {
        String key = VIDEO_STATUS_KEY_PREFIX + storyId;
        String value = stringRedisTemplate.opsForValue().get(key);
        
        if (value == null) {
            // Redis에 데이터가 없으면 상태 알 수 없음을 반환
            log.warn("Redis에서 비디오 상태를 찾을 수 없음: storyId={}", storyId);
            return null;
        }
        
        return VideoStatus.valueOf(value);
    }
    
    /**
     * 새로운 SSE 연결 생성
     *
     * @param storyId 스토리 ID
     * @param initialStatus 초기 상태 (컨트롤러에서 DB 조회하여 전달)
     * @return SseEmitter 객체
     */
    public SseEmitter subscribe(String storyId, VideoStatus initialStatus) {
        // 기존 연결이 있다면 제거
        SseEmitter existingEmitter = sseEmitterRepository.get(storyId);
        if (existingEmitter != null) {
            existingEmitter.complete();
            sseEmitterRepository.remove(storyId);
        }
        
        // 새 SseEmitter 생성
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        
        // 에러, 타임아웃, 완료 콜백 등록
        emitter.onCompletion(() -> {
            log.info("SSE 연결 완료: storyId={}", storyId);
            sseEmitterRepository.remove(storyId);
            lastSentProcessingSteps.remove(storyId); // 추적 데이터 정리
        });
        
        emitter.onTimeout(() -> {
            log.info("SSE 연결 타임아웃: storyId={}", storyId);
            emitter.complete();
            sseEmitterRepository.remove(storyId);
            lastSentProcessingSteps.remove(storyId); // 추적 데이터 정리
        });
        
        emitter.onError((e) -> {
            log.error("SSE 연결 오류: storyId={}, error={}", storyId, e.getMessage());
            emitter.complete();
            sseEmitterRepository.remove(storyId);
            lastSentProcessingSteps.remove(storyId); // 추적 데이터 정리
        });
        
        // 저장소에 emitter 저장
        sseEmitterRepository.save(storyId, emitter);
        
        // 초기 연결 설정을 위한 빈 데이터 전송
        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("연결됨 - 비디오 처리 상태 업데이트 대기 중"));
            
            // 초기 상태를 Redis에 저장 (컨트롤러에서 전달받은 상태 사용)
            if (initialStatus != null) {
                saveVideoStatus(storyId, initialStatus);
            }
            
            // 현재 상태 즉시 전송
            sendCurrentStatus(storyId);
        } catch (IOException e) {
            log.error("초기 SSE 메시지 전송 오류: {}", e.getMessage());
            emitter.completeWithError(e);
        }
        
        return emitter;
    }
    
    /**
     * 특정 SSE 클라이언트에 상태 업데이트 전송
     */
    public void sendStatusUpdateToClient(String storyId, VideoStatus status) {
        SseEmitter emitter = sseEmitterRepository.get(storyId);
        if (emitter != null) {
            try {
                VideoStatusResponseDto responseDto = new VideoStatusResponseDto();
                responseDto.setStatus(status);
                responseDto.setStoryId(storyId);
                
                // PROCESSING 상태인 경우 Redis에서 세부 단계 정보 가져옴
                if (status == VideoStatus.PROCESSING) {
                    VideoProcessingStep currentStep = videoProcessingStatusService.getProcessingStep(storyId);
                    if (currentStep != null) {
                        responseDto.setProcessingStep(currentStep.name());
                    }
                }
                
                // 상태가 PROCESSING이 아니거나 처리 단계가 변경된 경우에만 전송
                boolean shouldSendUpdate = false;
                
                if (status != VideoStatus.PROCESSING) {
                    // PROCESSING 외의 다른 상태 변경은 항상 전송
                    shouldSendUpdate = true;
                    // 처리가 완료되거나 실패한 경우 마지막 단계 추적 정보 삭제
                    if (status == VideoStatus.COMPLETED || status == VideoStatus.FAILED) {
                        lastSentProcessingSteps.remove(storyId);
                    }
                } else {
                    VideoProcessingStep currentStep = videoProcessingStatusService.getProcessingStep(storyId);
                    VideoProcessingStep lastSentStep = lastSentProcessingSteps.get(storyId);
                    
                    // 처리 단계가 변경되었거나 처음 전송하는 경우 업데이트 전송
                    if (currentStep != null && (lastSentStep == null || !lastSentStep.equals(currentStep))) {
                        shouldSendUpdate = true;
                        // 현재 단계 저장
                        lastSentProcessingSteps.put(storyId, currentStep);
                        log.info("처리 단계 변경 감지 - 업데이트 전송: storyId={}, previousStep={}, currentStep={}", 
                                storyId, lastSentStep, currentStep);
                    }
                }
                
                if (shouldSendUpdate) {
                    emitter.send(SseEmitter.event()
                            .name("status")
                            .data(responseDto));
                    
                    // 처리가 완료되거나 실패한 경우 SSE 연결 종료
                    if (status == VideoStatus.COMPLETED || status == VideoStatus.FAILED) {
                        log.info("비디오 처리 {} - SSE 연결 종료: storyId={}", status, storyId);
                        emitter.complete();
                        sseEmitterRepository.remove(storyId);
                    }
                }
            } catch (IOException e) {
                log.error("SSE 상태 메시지 전송 오류: storyId={}, error={}", storyId, e.getMessage());
                emitter.completeWithError(e);
            }
        }
    }
    
    /**
     * 현재 비디오 처리 상태를 전송
     *
     * @param storyId 스토리 ID
     */
    public void sendCurrentStatus(String storyId) {
        SseEmitter emitter = sseEmitterRepository.get(storyId);
        if (emitter == null) {
            log.warn("SSE 연결을 찾을 수 없음: storyId={}", storyId);
            return;
        }
        
        try {
            // Redis에서 비디오 상태 조회 (DB 조회 없음)
            VideoStatus status = getVideoStatusFromRedis(storyId);
            
            if (status == null) {
                // Redis에 상태가 없으면 SSE 연결 종료
                log.warn("상태 정보가 없어 SSE 연결을 종료합니다: storyId={}", storyId);
                emitter.complete();
                sseEmitterRepository.remove(storyId);
                lastSentProcessingSteps.remove(storyId);
                return;
            }
            
            VideoStatusResponseDto responseDto = new VideoStatusResponseDto();
            responseDto.setStatus(status);
            responseDto.setStoryId(storyId);
            
            // PROCESSING 상태인 경우 Redis에서 세부 단계 정보 가져옴
            if (status == VideoStatus.PROCESSING) {
                VideoProcessingStep currentStep = videoProcessingStatusService.getProcessingStep(storyId);
                if (currentStep != null) {
                    responseDto.setProcessingStep(currentStep.name());
                    // 처음 연결 시 마지막 단계 업데이트
                    if (!lastSentProcessingSteps.containsKey(storyId)) {
                        lastSentProcessingSteps.put(storyId, currentStep);
                    }
                }
            }
            
            // SSE 이벤트로 상태 전송
            emitter.send(SseEmitter.event()
                    .name("status")
                    .data(responseDto));
            
            // 처리가 완료되거나 실패한 경우 SSE 연결 종료
            if (status == VideoStatus.COMPLETED || status == VideoStatus.FAILED) {
                log.info("비디오 처리 {} - SSE 연결 종료: storyId={}", status, storyId);
                emitter.complete();
                sseEmitterRepository.remove(storyId);
                lastSentProcessingSteps.remove(storyId);
            }
        } catch (IOException e) {
            log.error("SSE 상태 메시지 전송 오류: storyId={}, error={}", storyId, e.getMessage());
            emitter.completeWithError(e);
        } catch (Exception e) {
            log.error("비디오 상태 조회 오류: storyId={}, error={}", storyId, e.getMessage());
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data("상태 조회 오류: " + e.getMessage()));
            } catch (IOException ex) {
                emitter.completeWithError(ex);
            }
        }
    }
    
    /**
     * SSE 연결 종료
     */
    public void complete(String storyId) {
        SseEmitter emitter = sseEmitterRepository.get(storyId);
        if (emitter != null) {
            log.info("SSE 연결 종료 요청: storyId={}", storyId);
            emitter.complete();
            sseEmitterRepository.remove(storyId);
            lastSentProcessingSteps.remove(storyId);
        }
    }
} 