package com.sss.backend.domain.repository;

import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE 연결을 저장하고 관리하는 리포지토리
 */
@Repository
public class SseEmitterRepository {

    // storyId를 키로, SseEmitter를 값으로 저장하는 ConcurrentHashMap
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * 새 SseEmitter를 저장
     *
     * @param storyId 스토리 ID
     * @param emitter 저장할 SseEmitter
     * @return 저장된 SseEmitter
     */
    public SseEmitter save(String storyId, SseEmitter emitter) {
        emitters.put(storyId, emitter);
        return emitter;
    }

    /**
     * 특정 storyId에 해당하는 SseEmitter 조회
     *
     * @param storyId 스토리 ID
     * @return SseEmitter 객체 또는 null
     */
    public SseEmitter get(String storyId) {
        return emitters.get(storyId);
    }

    /**
     * 특정 storyId에 해당하는 SseEmitter 삭제
     *
     * @param storyId 스토리 ID
     */
    public void remove(String storyId) {
        emitters.remove(storyId);
    }

    /**
     * 모든 SseEmitter 반환
     *
     * @return SseEmitter 맵
     */
    public Map<String, SseEmitter> getAllEmitters() {
        return emitters;
    }
} 