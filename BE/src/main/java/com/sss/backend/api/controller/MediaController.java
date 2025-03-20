package com.sss.backend.api.controller;


import com.sss.backend.api.dto.MediaProcessRequest;
import com.sss.backend.api.dto.MediaProcessResponse;
import com.sss.backend.api.dto.SceneImageRequest;
import com.sss.backend.domain.document.SceneDocument;
import com.sss.backend.domain.service.AudioService;
import com.sss.backend.domain.service.ImageService;
import com.sss.backend.domain.service.MediaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/videos/generate")
@Slf4j
public class MediaController {

    private final MediaService mediaService;
    private final AudioService audioService;
    private final ImageService imageService;

    public MediaController(MediaService mediaService, AudioService audioService, ImageService imageService) {
        this.mediaService = mediaService;
        this.audioService = audioService;
        this.imageService = imageService;
    }



    //특정 씬의 오디오만 생성 요청
    @PostMapping("/story/{storyId}/scene/{sceneId}/audio")
    public ResponseEntity<MediaProcessResponse> generateSceneAudio(
            @PathVariable String storyId,
            @PathVariable Integer sceneId) {
        log.info("씬 오디오 생성 요청: storyId={}, sceneId={}", storyId, sceneId);

        try {
            SceneDocument result = audioService.generateSceneAudio(storyId, sceneId);

            return ResponseEntity.ok(new MediaProcessResponse(
                    true,
                    "씬 오디오 생성 요청이 성공적으로 처리되었습니다.",
                    "AUDIO_READY"));
        } catch (Exception e) {
            log.error("씬 오디오 생성 요청 처리 중 오류 발생: storyId={}, sceneId={}, error={}",
                    storyId, sceneId, e.getMessage(), e);

            return ResponseEntity.internalServerError().body(new MediaProcessResponse(
                    false,
                    "씬 오디오 생성 요청 처리 중 오류가 발생했습니다: " + e.getMessage(),
                    "AUDIO_ERROR"));
        }
    }


    //특정 씬의 이미지 생성 요청
    @PostMapping("/story/{storyId}/scene/{sceneId}/image")
    public ResponseEntity<MediaProcessResponse> generateImage(
            @PathVariable String storyId,
            @PathVariable Integer sceneId,
            @RequestBody(required = false) SceneImageRequest imageRequest) {
        log.info("이미지 생성 요청: storyId={}, sceneId={}", storyId, sceneId);

        try {
            // 요청 객체가 null이면 새로 생성
            if (imageRequest == null) {
                imageRequest = new SceneImageRequest();
            }

            // scene_id 설정
            imageRequest.setSceneId(sceneId);

            // 이미지 서비스 호출
            imageService.generateImage(imageRequest, storyId)
                    .thenAccept(response -> {
                        log.info("이미지 생성 완료: storyId={}, sceneId={}, url={}",
                                storyId, sceneId, response.getImage_url());
                    });

            return ResponseEntity.ok(new MediaProcessResponse(
                    true,
                    "이미지 생성 요청이 성공적으로 처리되었습니다.",
                    "IMAGE_PROCESSING"));
        } catch (Exception e) {
            log.error("이미지 생성 요청 처리 중 오류 발생: storyId={}, sceneId={}, error={}",
                    storyId, sceneId, e.getMessage(), e);

            return ResponseEntity.internalServerError().body(new MediaProcessResponse(
                    false,
                    "이미지 생성 요청 처리 중 오류가 발생했습니다: " + e.getMessage(),
                    "IMAGE_ERROR"));
        }
    }

    //스토리의 모든 씬 요청
    @PostMapping("/story/{storyId}/process-all")
    public ResponseEntity<MediaProcessResponse> processAllScenes(@PathVariable String storyId) {
        log.info("스토리 전체 씬 미디어 처리 요청: storyId={}", storyId);

        try {
            // MediaService의 processAllScenes 메서드 호출
            CompletableFuture<Void> future = mediaService.processAllScenes(storyId);

            return ResponseEntity.ok(new MediaProcessResponse(
                    true,
                    "스토리 전체 씬 미디어 처리 요청이 성공적으로 처리되었습니다.",
                    "MEDIA_PROCESSING"));
        } catch (Exception e) {
            log.error("스토리 전체 씬 미디어 처리 요청 중 오류 발생: storyId={}, error={}",
                    storyId, e.getMessage(), e);

            return ResponseEntity.internalServerError().body(new MediaProcessResponse(
                    false,
                    "스토리 전체 씬 미디어 처리 요청 중 오류가 발생했습니다: " + e.getMessage(),
                    "MEDIA_ERROR"));
        }
    }

    /**
     * 특정 씬 미디어(오디오, 이미지) 생성 요청
     *
     * @param storyId 스토리 ID
     * @param sceneId 씬 ID
     * @return 처리 상태
     */
    @PostMapping("/story/{storyId}/scene/{sceneId}")
    public ResponseEntity<MediaProcessResponse> processSceneMedia(
            @PathVariable String storyId,
            @PathVariable Integer sceneId) {
        log.info("씬 미디어 생성 요청: storyId={}, sceneId={}", storyId, sceneId);

        try {
            CompletableFuture<Void> future = mediaService.processSceneMedia(storyId, sceneId);

            return ResponseEntity.ok(new MediaProcessResponse(
                    true,
                    "씬 미디어 생성 요청이 성공적으로 처리되었습니다.",
                    "MEDIA_PROCESSING"));
        } catch (Exception e) {
            log.error("씬 미디어 생성 요청 처리 중 오류 발생: storyId={}, sceneId={}, error={}",
                    storyId, sceneId, e.getMessage(), e);

            return ResponseEntity.internalServerError().body(new MediaProcessResponse(
                    false,
                    "씬 미디어 생성 요청 처리 중 오류가 발생했습니다: " + e.getMessage(),
                    "MEDIA_ERROR"));
        }
    }



}
