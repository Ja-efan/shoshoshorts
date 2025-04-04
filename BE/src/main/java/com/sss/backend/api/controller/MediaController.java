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

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    //스토리의 모든 씬 요청
    @PostMapping("/story/{storyId}/process-all")
    public ResponseEntity<MediaProcessResponse> processAllScenes(@PathVariable String storyId, @RequestParam("audioModelName")String audioModelName, @RequestParam("imageModelName")String imageModelName) {
        log.info("스토리 전체 씬 미디어 처리 요청: storyId={}", storyId);

        log.info("fjfjfjfjfjfj");
        try {
            // MediaService의 processAllScenes 메서드 호출
            CompletableFuture<Void> future = mediaService.processAllScenes(storyId, audioModelName, imageModelName);

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

}
