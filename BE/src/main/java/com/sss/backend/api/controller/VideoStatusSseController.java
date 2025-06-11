package com.sss.backend.api.controller;

import com.sss.backend.domain.entity.Video.VideoStatus;
import com.sss.backend.domain.service.VideoService;
import com.sss.backend.domain.service.VideoStatusSseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 비디오 처리 상태를 SSE로 전송하는 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/video/status/sse")
@RequiredArgsConstructor
public class VideoStatusSseController {

    private final VideoStatusSseService videoStatusSseService;
    private final VideoService videoService;

    /**
     * 비디오 처리 상태에 대한 SSE 구독
     *
     * @param storyId 스토리 ID
     * @return SseEmitter 객체
     */
    @GetMapping(value = "/{storyId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeToStatus(@PathVariable String storyId) {
        log.info("SSE 연결 요청: storyId={}", storyId);
        
        try {
            // 먼저 Redis에서 상태를 확인 (DB 접근 없이)
            VideoStatus status = videoStatusSseService.getVideoStatusFromRedis(storyId);
            
            // Redis에 상태가 없는 경우에만 DB 조회
            if (status == null) {
                log.info("Redis에 상태 없음. DB에서 비디오 상태 확인: storyId={}", storyId);
                var dbStatus = videoService.getVideoStatus(storyId);
                status = dbStatus.getStatus();
                
                // Redis에 상태 저장 (향후 DB 조회 없이 사용하기 위함)
                videoStatusSseService.saveVideoStatus(storyId, status);
            } else {
                log.info("Redis에서 상태 조회 성공: storyId={}, status={}", storyId, status);
            }
            
            // PROCESSING 상태가 아니면 SSE 필요 없음
            if (status != VideoStatus.PROCESSING && status != VideoStatus.PENDING) {
                log.info("비디오가 이미 {} 상태입니다. SSE 필요 없음: storyId={}", status, storyId);
                // 빈 SseEmitter 반환 후 즉시 완료 처리
                SseEmitter emitter = new SseEmitter(0L);
                emitter.complete();
                return emitter;
            }
            
            // SseEmitter 생성 및 구독 (초기 상태 전달)
            return videoStatusSseService.subscribe(storyId, status);
        } catch (Exception e) {
            log.error("SSE 구독 중 오류 발생: storyId={}, error={}", storyId, e.getMessage(), e);
            // 오류 시 빈 SseEmitter 반환
            SseEmitter emitter = new SseEmitter(0L);
            emitter.completeWithError(e);
            return emitter;
        }
    }

    /**
     * SSE 연결 수동 종료
     *
     * @param storyId 스토리 ID
     * @return 응답 엔티티
     */
    @DeleteMapping("/{storyId}")
    public ResponseEntity<String> unsubscribe(@PathVariable String storyId) {
        log.info("SSE 연결 종료 요청: storyId={}", storyId);
        videoStatusSseService.complete(storyId);
        return ResponseEntity.ok("SSE 연결이 종료되었습니다.");
    }
} 