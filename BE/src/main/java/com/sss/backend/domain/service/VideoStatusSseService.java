package com.sss.backend.domain.service;

import com.sss.backend.api.dto.VideoStatusResponseDto;
import com.sss.backend.domain.entity.Video.VideoStatus;
import com.sss.backend.domain.entity.VideoProcessingStep;
import com.sss.backend.domain.repository.SseEmitterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
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
    private final VideoService videoService;
    
    // SSE 연결 타임아웃 (30분)
    private static final long SSE_TIMEOUT = 30 * 60 * 1000L;
    
    // 상태 업데이트 주기 (초)
    private static final long STATUS_UPDATE_INTERVAL = 1;
    
    // 스케줄링을 위한 실행자
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);
    
    /**
     * 새로운 SSE 연결 생성
     *
     * @param storyId 스토리 ID
     * @return SseEmitter 객체
     */
    public SseEmitter subscribe(String storyId) {
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
        });
        
        emitter.onTimeout(() -> {
            log.info("SSE 연결 타임아웃: storyId={}", storyId);
            emitter.complete();
            sseEmitterRepository.remove(storyId);
        });
        
        emitter.onError((e) -> {
            log.error("SSE 연결 오류: storyId={}, error={}", storyId, e.getMessage());
            emitter.complete();
            sseEmitterRepository.remove(storyId);
        });
        
        // 저장소에 emitter 저장
        sseEmitterRepository.save(storyId, emitter);
        
        // 초기 연결 설정을 위한 빈 데이터 전송
        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("연결됨 - 비디오 처리 상태 업데이트 대기 중"));
            
            // 현재 상태 즉시 전송
            sendCurrentStatus(storyId);
            
            // 주기적 상태 업데이트를 위한 스케줄러 시작
            scheduleStatusUpdates(storyId);
        } catch (IOException e) {
            log.error("초기 SSE 메시지 전송 오류: {}", e.getMessage());
            emitter.completeWithError(e);
        }
        
        return emitter;
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
            // 비디오 상태 조회
            VideoStatusResponseDto status = videoService.getVideoStatus(storyId);
            
            // PROCESSING 상태인 경우 Redis에서 세부 단계 정보 가져옴
            if (status.getStatus() == VideoStatus.PROCESSING) {
                VideoProcessingStep step = videoProcessingStatusService.getProcessingStep(storyId);
                if (step != null) {
                    status.setProcessingStep(step.name());
                }
            }
            
            // SSE 이벤트로 상태 전송
            emitter.send(SseEmitter.event()
                    .name("status")
                    .data(status));
            
            // 처리가 완료되거나 실패한 경우 SSE 연결 종료
            if (status.getStatus() == VideoStatus.COMPLETED || status.getStatus() == VideoStatus.FAILED) {
                log.info("비디오 처리 {} - SSE 연결 종료: storyId={}", status.getStatus(), storyId);
                emitter.complete();
                sseEmitterRepository.remove(storyId);
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
     * 주기적으로 상태 업데이트를 보내도록 스케줄링
     *
     * @param storyId 스토리 ID
     */
    private void scheduleStatusUpdates(String storyId) {
        scheduler.scheduleAtFixedRate(() -> {
            SseEmitter emitter = sseEmitterRepository.get(storyId);
            if (emitter == null) {
                // 스케줄링된 작업 종료
                throw new RuntimeException("Emitter not found - cancel scheduling");
            }
            
            try {
                sendCurrentStatus(storyId);
            } catch (Exception e) {
                log.error("스케줄된 상태 업데이트 오류: storyId={}, error={}", storyId, e.getMessage());
                emitter.completeWithError(e);
                throw new RuntimeException("Error in scheduled update - cancel scheduling", e);
            }
        }, 0, STATUS_UPDATE_INTERVAL, TimeUnit.SECONDS);
    }
    
    /**
     * 특정 스토리에 대한 SSE 연결 종료
     *
     * @param storyId 스토리 ID
     */
    public void complete(String storyId) {
        SseEmitter emitter = sseEmitterRepository.get(storyId);
        if (emitter != null) {
            emitter.complete();
            sseEmitterRepository.remove(storyId);
            log.info("SSE 연결 수동 종료: storyId={}", storyId);
        }
    }
} 