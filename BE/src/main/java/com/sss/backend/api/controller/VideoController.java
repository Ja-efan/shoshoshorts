package com.sss.backend.api.controller;

import com.sss.backend.api.dto.StoryRequestDTO;
import com.sss.backend.api.dto.VideoResponseDto;
import com.sss.backend.domain.service.StoryService;
import com.sss.backend.domain.service.MediaService;
import com.sss.backend.domain.service.VideoService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

//@Async
@RestController
@Slf4j
@RequestMapping("/api/videos")
public class VideoController {

    @Autowired
    private MediaService mediaService;

    @Autowired
    private VideoService videoService;

    @Value("${temp.directory}")
    private String tempDirectory;
    private StoryService storyService;

    public VideoController(StoryService storyService) {
        this.storyService = storyService;
    }

    @PostMapping("/generate-sync")
    public ResponseEntity<VideoResponseDto> generateVideoSync(@Valid @RequestBody StoryRequestDTO request) throws Exception{
        // 입력 데이터 json 파싱
        Long storyId = storyService.saveStory(request);
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
} 