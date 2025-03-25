package com.sss.backend.api.controller;

import com.sss.backend.api.dto.*;
import com.sss.backend.config.S3Config;
import com.sss.backend.domain.entity.Video.VideoStatus;
import com.sss.backend.domain.service.MediaService;
import com.sss.backend.domain.service.StoryService;
import com.sss.backend.domain.service.VideoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

//@Async
@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/videos")
public class VideoController {

    private final MediaService mediaService;
    private final VideoService videoService;
    private final StoryService storyService;
    private final S3Config s3Config;

    @Value("${temp.directory}")
    private String tempDirectory;

    // 비동기 비디오 생성 요청
    @PostMapping("/generate")
    public ResponseEntity<VideoStatusResponseDto> generateVideoAsync(@Valid @RequestBody StoryRequestDTO request) {
        try {
            // 스토리 저장
            log.info("안녀안녕");
            Long storyId = storyService.saveBasicStory(request);
            log.info("스토리 엔티티 생성 완료: {}", storyId);
            
            // 비디오 엔티티 초기 상태로 저장
            videoService.initVideoEntity(storyId.toString());
            
            // 비동기 처리 시작
            CompletableFuture.runAsync(() -> {
                try {
                    storyService.saveStory(storyId, request);
                    log.info("스토리 스크립트 생성 완료");
                    // 상태 업데이트: 처리 중
                    videoService.updateVideoStatus(storyId.toString(), VideoStatus.PROCESSING, null);
                    
                    // 미디어 생성 처리
                    CompletableFuture<Void> future = mediaService.processAllScenes(storyId.toString());
                    future.get(30, TimeUnit.MINUTES);
                    
                    // 비디오 생성 및 업로드
                    String outputPath = tempDirectory + "/" + UUID.randomUUID() + "_final.mp4";
                    String videoUrl = videoService.createAndUploadVideo(storyId.toString(), outputPath);
                    
                    // 상태 업데이트: 완료
                    videoService.updateVideoStatus(storyId.toString(), VideoStatus.COMPLETED, videoUrl);
                    
                } catch (Exception e) {
                    log.error("비디오 생성 중 오류 발생: {}", e.getMessage(), e);
                    // 상태 업데이트: 실패
                    videoService.updateVideoStatus(storyId.toString(), VideoStatus.FAILED, e.getMessage());
                }
            });
            
            // 초기 응답 반환 (storyId, status, createdAt 포함)
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String createdAt = java.time.LocalDateTime.now().format(formatter);
            VideoStatusResponseDto response = new VideoStatusResponseDto(storyId.toString(), VideoStatus.PENDING, createdAt);
            return ResponseEntity.accepted().body(response);
            
        } catch (Exception e) {
            log.error("비디오 생성 요청 처리 중 오류: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // 비디오 상태 조회 API
    @GetMapping("/status/{storyId}")
    public ResponseEntity<VideoStatusResponseDto> getVideoStatus(@PathVariable String storyId) {
        try {
            VideoStatusResponseDto status = videoService.getVideoStatus(storyId);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("비디오 상태 조회 중 오류: {}", e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }

    // 전체 비디오 상태 조회 API
    @GetMapping("/status/allstory")
    public ResponseEntity<VideoListResponseDTO> getAllVideoStatus() {
        try {
            VideoListResponseDTO response = videoService.getAllVideoStatus();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("비디오 상태 조회 중 오류 {} ", e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }


    /**
    // 동기 메서드
    @PostMapping("/generate-sync")
    public ResponseEntity<VideoResponseDto> generateVideoSync(@Valid @RequestBody StoryRequestDTO request) throws Exception {
        // 입력 데이터 json 파싱
        // 스토리 저장 로직 불러오기
        Long storyId = 1;
        System.out.println("스토리 생성 완료: " + storyId);


        // 이미지, 음성 생성
        // MediaService의 processAllScenes 메서드 호출
        CompletableFuture<Void> future = mediaService.processAllScenes(storyId.toString());
        future.get(30, TimeUnit.MINUTES); // 타임아웃 설정 (예: 30분)
        System.out.println("미디어 생성 완료: " + storyId);


        // 비디오 생성 및 S3 업로드
        String outputPath = tempDirectory + "/" + UUID.randomUUID() + "_final.mp4";  // 임시 출력 파일 경로
        String videoUrl = videoService.createAndUploadVideo(storyId.toString(), outputPath);

        VideoResponseDto response = new VideoResponseDto(storyId.toString(), videoUrl);
        return ResponseEntity.ok(response);
    }
*/

    @GetMapping("/{storyId}")
    public ResponseEntity<VideoResponseDto> generateVideo(@PathVariable String storyId) {
        // 임시 출력 파일 경로 생성
        String outputPath = tempDirectory + "/" + UUID.randomUUID() + "_final.mp4";

        // 비디오 생성 및 S3 업로드
        String videoUrl = videoService.createAndUploadVideo(storyId, outputPath);

        // DTO 응답 생성
        VideoResponseDto response = new VideoResponseDto(storyId, videoUrl);

        return ResponseEntity.ok(response);
    }

    // 영상 다운로드
    @GetMapping("/download/{storyId}")
    public ResponseEntity<Object> downloadVideo(@PathVariable String storyId) {
        try {
            // 비디오 상태 조회
            VideoStatusResponseDto videoStatus = videoService.getVideoStatus(storyId);

            // 비디오가 완료 상태가 아니거나 URL이 없는 경우
            if (videoStatus.getStatus() != VideoStatus.COMPLETED || videoStatus.getVideoUrl() == null) {
                return ResponseEntity.notFound().build();
            }

            // S3 키 추출
            String s3Key = s3Config.extractS3KeyFromUrl(videoStatus.getVideoUrl());

            // 파일명 설정
            String fileName = "shoshoshorts_" +
                java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Seoul"))
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) +
                ".mp4";

            // 다운로드용 Presigned URL
            String presignedUrl = s3Config.generateDownloadPresignedUrl(s3Key, fileName);

            // Presigned URL로 리다이렉트
            return ResponseEntity.status(302)
                    .header(HttpHeaders.LOCATION, presignedUrl)
                    .build();

        } catch (Exception e) {
            log.error("비디오 다운로드 중 오류: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}